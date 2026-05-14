package com.xcvk.platform.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcvk.platform.api.contract.auth.client.AuthUserClient;
import com.xcvk.platform.api.contract.auth.model.InternalUserInfoResponse;
import com.xcvk.platform.api.contract.workflow.model.QueryAiTicketItem;
import com.xcvk.platform.api.contract.workflow.model.QueryAiTicketRequest;
import com.xcvk.platform.api.contract.workflow.model.QueryAiTicketResponse;
import com.xcvk.platform.auth.starter.constant.PlatformRoleConstants;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.common.enums.CommonStatusEnum;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.common.util.DbAssert;
import com.xcvk.platform.id.generator.SnowflakeIdGenerator;
import com.xcvk.platform.workflow.assembler.TicketAssembler;
import com.xcvk.platform.workflow.constant.TicketErrorMessages;
import com.xcvk.platform.workflow.constant.TicketSourceConstants;
import com.xcvk.platform.workflow.constant.TicketStatusConstants;
import com.xcvk.platform.workflow.domain.TicketStatusMachine;
import com.xcvk.platform.workflow.model.cmd.CreateTicketCmd;
import com.xcvk.platform.workflow.model.dto.AssignTicketRequest;
import com.xcvk.platform.workflow.model.dto.UpdateTicketStatusRequest;
import com.xcvk.platform.workflow.model.entity.Ticket;
import com.xcvk.platform.workflow.model.entity.TicketType;
import com.xcvk.platform.workflow.model.query.MyTicketQuery;
import com.xcvk.platform.workflow.model.query.TicketManageQuery;
import com.xcvk.platform.workflow.model.vo.*;
import com.xcvk.platform.workflow.repository.mapper.TicketMapper;
import com.xcvk.platform.workflow.search.assembler.TicketSearchAssembler;
import com.xcvk.platform.workflow.search.model.index.TicketIndex;
import com.xcvk.platform.workflow.search.repository.TicketIndexRepository;
import com.xcvk.platform.workflow.service.SearchSyncTaskService;
import com.xcvk.platform.workflow.service.TicketEventService;
import com.xcvk.platform.workflow.service.TicketService;
import com.xcvk.platform.workflow.service.TicketTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 工单服务实现类
 *
 * <p>当前阶段围绕工单主链提供两类能力：</p>
 * <ul>
 *     <li>员工侧：创建工单、我的工单列表、我的工单详情</li>
 *     <li>处理侧：支持人员/管理员视角的工单列表、接单</li>
 * </ul>
 *
 * <p>实现上优先遵循“主链优先、低风险、先闭环再增强”的原则。
 * 因此当前阶段处理侧能力先基于工单主表当前态实现，
 * 不提前引入过重的历史表或复杂状态机。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements TicketService {

    private static final String DEFAULT_PRIORITY = "MEDIUM";


    private static final int DEFAULT_PAGE_NUM = 1;

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final TicketTypeService ticketTypeService;
    private final SnowflakeIdGenerator idGenerator;
    private final TicketAssembler ticketAssembler;
    private final TicketIndexRepository ticketIndexRepository;
    private final TicketSearchAssembler ticketSearchAssembler;
    private final TicketEventService ticketEventService;
    private final AuthUserClient authUserClient;
    private final SearchSyncTaskService searchSyncTaskService;

    /**
     * 创建工单主流程。
     *
     * @param creatorId 创建人ID
     * @param creatorName 创建人姓名
     * @param ticketTypeCode 工单类型编码
     * @param title 工单标题
     * @param content 工单内容
     * @param priority 优先级
     * @return 工单创建结果
     */
    // TODO 后续修改逻辑为es查询失败降级MySQL
    // TODO es后续改为异步
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateTicketResponse createTicket(Long creatorId, String creatorName,
                                             String ticketTypeCode, String title,
                                             String content, String priority) {
        CreateTicketCmd cmd = ticketAssembler.toCreateTicketCmd(
                creatorId, creatorName, ticketTypeCode, title, content, priority
        );

        validateCreateCmd(cmd);
        TicketType ticketType = ticketTypeService.getEnabledTicketType(cmd.ticketTypeCode());
        validateTicketSource(cmd, ticketType);

        Long ticketId = idGenerator.nextId();
        String ticketNo = buildTicketNo(ticketId);

        Ticket ticket = buildTicket(cmd, ticketType, ticketId, ticketNo);

        int rows = baseMapper.insert(ticket);
        DbAssert.affectedOne(rows, TicketErrorMessages.CREATE_FAILED);

        ticketEventService.recordCreateEvent(ticket, creatorId, creatorName);

        searchSyncTaskService.enqueueTicketUpsert(ticketId);

        return new CreateTicketResponse(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getStatus()
        );
    }

    /**
     * 同步工单到搜索索引。
     *
     * <p>当前阶段 Elasticsearch 作为搜索副本，不属于主业务真相来源，
     * 因此同步失败不直接中断主业务流程，而是记录错误日志，便于后续排查和补偿。</p>
     *
     * <p>后续如引入搜索补偿任务表或 MQ 异步同步机制，
     * 可在 catch 分支中扩展失败记录逻辑。</p>
     *
     * @param ticket 工单实体
     */
    private void syncTicketToSearchIndex(Ticket ticket) {
        if (ticket == null) {
            return;
        }

        try {
            ticketIndexRepository.save(ticketSearchAssembler.toIndex(ticket));
            log.info("同步工单到搜索索引成功：{}", ticket.getId());
        } catch (Exception ex) {
            log.error("同步工单到搜索索引失败，ticketId={}", ticket.getId(), ex);
        }
    }

    /**
     * 同步工单到搜索索引。
     *
     * <p>该方法由搜索同步任务处理器调用。这里不要吞掉异常，
     * 否则外层 SearchSyncTaskProcessor 无法感知失败，也就无法进入重试流程。</p>
     *
     * @param ticketId 工单ID
     */
    @Override
    public void syncTicketToSearchIndex(Long ticketId) {
        BizAssert.notNull(ticketId, ErrorCode.PARAM_INVALID, TicketErrorMessages.TICKET_ID_REQUIRED);

        Ticket latestTicket = getById(ticketId);
        BizAssert.notNull(latestTicket, ErrorCode.BIZ_ERROR, TicketErrorMessages.TICKET_NOT_FOUND);

        TicketIndex index = ticketSearchAssembler.toIndex(latestTicket);
        ticketIndexRepository.save(index);

        log.info("同步工单到搜索索引成功：{}", ticketId);
    }



    /**
     * 校验创建工单命令对象的基础必填字段。
     *
     * @param cmd 创建工单命令对象
     */
    private void validateCreateCmd(CreateTicketCmd cmd) {
        BizAssert.notNull(cmd, ErrorCode.PARAM_INVALID, TicketErrorMessages.CREATE_CMD_REQUIRED);

        BizAssert.hasText(cmd.ticketTypeCode(), ErrorCode.PARAM_INVALID, TicketErrorMessages.TICKET_TYPE_REQUIRED);
        BizAssert.hasText(cmd.title(), ErrorCode.PARAM_INVALID, TicketErrorMessages.TITLE_REQUIRED);
        BizAssert.hasText(cmd.content(), ErrorCode.PARAM_INVALID, TicketErrorMessages.CONTENT_REQUIRED);
        BizAssert.hasText(cmd.creatorName(), ErrorCode.PARAM_INVALID, TicketErrorMessages.CREATOR_NAME_REQUIRED);
        BizAssert.notNull(cmd.creatorId(), ErrorCode.PARAM_INVALID, TicketErrorMessages.CREATOR_ID_REQUIRED);
        BizAssert.hasText(cmd.source(), ErrorCode.PARAM_INVALID, TicketErrorMessages.SOURCE_REQUIRED);
    }

    /**
     * 构建工单编号。
     *
     * @param ticketId 工单ID
     * @return 工单编号
     */
    private String buildTicketNo(Long ticketId) {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String sequencePart = String.format("%06d", Math.abs(ticketId % 1_000_000));
        return "TK" + datePart + sequencePart;
    }

    /**
     * 校验工单来源是否合法，并在 AI 创建场景下校验类型是否允许 AI 发起。
     *
     * @param cmd 创建工单命令对象
     * @param ticketType 工单类型对象
     */
    private void validateTicketSource(CreateTicketCmd cmd, TicketType ticketType) {
        BizAssert.isTrue(
                TicketSourceConstants.MANUAL.equals(cmd.source())
                        || TicketSourceConstants.AI_AGENT.equals(cmd.source()),
                ErrorCode.PARAM_INVALID,
                TicketErrorMessages.SOURCE_INVALID
        );

        if (TicketSourceConstants.AI_AGENT.equals(cmd.source())) {
            BizAssert.isTrue(
                    CommonStatusEnum.isEnabled(ticketType.getAllowAiCreate()),
                    ErrorCode.BIZ_ERROR,
                    TicketErrorMessages.AI_CREATE_NOT_ALLOWED
            );
        }
    }

    /**
     * 对字符串做基础清洗，避免首尾空格进入数据库。
     *
     * @param value 原始字符串
     * @return 去除首尾空格后的字符串
     */
    private String safeTrim(String value) {
        return value != null ? value.trim() : null;
    }

    /**
     * 解析工单优先级。
     *
     * @param priority 显式传入优先级
     * @param defaultPriority 工单类型默认优先级
     * @return 最终优先级
     */
    private String resolvePriority(String priority, String defaultPriority) {
        if (StringUtils.hasText(priority)) {
            return priority.trim();
        }
        if (StringUtils.hasText(defaultPriority)) {
            return defaultPriority.trim();
        }
        return DEFAULT_PRIORITY;
    }

    /**
     * 构建工单实体。
     *
     * @param cmd 创建工单命令对象
     * @param ticketType 工单类型对象
     * @param ticketId 工单ID
     * @param ticketNo 工单编号
     * @return 工单实体
     */
    // TODO 后续看情况提取builder/factory
    private Ticket buildTicket(CreateTicketCmd cmd, TicketType ticketType, Long ticketId, String ticketNo) {
        return new Ticket()
                .setId(ticketId)
                .setTicketNo(ticketNo)
                .setTicketTypeId(ticketType.getId())
                .setTicketTypeCode(ticketType.getTypeCode())
                .setTicketTypeName(ticketType.getTypeName())
                .setTitle(safeTrim(cmd.title()))
                .setContent(safeTrim(cmd.content()))
                .setStatus(TicketStatusConstants.PENDING)
                .setPriority(resolvePriority(cmd.priority(), ticketType.getDefaultPriority()))
                .setSource(safeTrim(cmd.source()))
                .setSourceRef(safeTrim(cmd.sourceRef()))
                .setCreatorId(cmd.creatorId())
                .setCreatorName(safeTrim(cmd.creatorName()));
    }

    /**
     * 分页查询当前登录用户创建的工单列表。
     *
     * @param creatorId 创建人ID
     * @param query 查询条件
     * @return 工单分页结果
     */
    @Override
    public PageResult<TicketListItemVO> pageMyTickets(Long creatorId, MyTicketQuery query) {
        BizAssert.notNull(creatorId, ErrorCode.PARAM_INVALID, TicketErrorMessages.CREATOR_ID_REQUIRED);
        BizAssert.notNull(query, ErrorCode.PARAM_INVALID, TicketErrorMessages.QUERY_REQUIRED);

        int pageNum = query.safePageNum();
        int pageSize = query.safePageSize();

        LambdaQueryWrapper<Ticket> qw = buildMyTicketQueryWrapper(creatorId, query);
        qw.orderByDesc(Ticket::getCreatedAt);

        Page<Ticket> page = this.page(new Page<>(pageNum, pageSize), qw);

        List<TicketListItemVO> records = page.getRecords().stream()
                .map(ticketAssembler::toTicketListItemVO)
                .toList();

        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    /**
     * 构建“我的工单”分页查询条件。
     *
     * @param creatorId 创建人ID
     * @param query 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapper<Ticket> buildMyTicketQueryWrapper(Long creatorId, MyTicketQuery query) {
        LambdaQueryWrapper<Ticket> qw = new LambdaQueryWrapper<>();
        qw.eq(Ticket::getCreatorId, creatorId);

        if (StringUtils.hasText(query.status())) {
            qw.eq(Ticket::getStatus, query.status().trim());
        }

        return qw;
    }

    /**
     * 查询工单详情。
     *
     * <p>管理员可以查看所有工单；
     * 非管理员只能查看自己创建的工单。</p>
     *
     * @param identity 当前登录身份
     * @param ticketId 工单ID
     * @return 工单详情
     */
    @Override
    public TicketDetailVO getMyTicketDetail(CurrentLoginIdentity identity, Long ticketId) {
        validateCurrentLoginIdentity(identity);
        BizAssert.notNull(ticketId, ErrorCode.PARAM_INVALID, TicketErrorMessages.TICKET_ID_REQUIRED);

        Long currentUserId = identity.userId();
        List<String> roleCodes = identity.roleCodes();

        boolean isAdmin = hasRole(roleCodes, PlatformRoleConstants.ADMIN);

        LambdaQueryWrapper<Ticket> qw = new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getId, ticketId);

        if (!isAdmin) {
            qw.eq(Ticket::getCreatorId, currentUserId);
        }

        Ticket ticket = this.getOne(qw, false);

        BizAssert.notNull(
                ticket,
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.TICKET_NOT_FOUND_OR_NO_PERMISSION
        );

        return ticketAssembler.toTicketDetailVO(ticket);
    }


    /**
     * 查询处理侧工单详情。
     *
     * <p>管理员可以查看所有工单；
     * 支持人员只能查看未分派工单或自己处理中的工单，
     * 避免支持人员越权查看其他处理人已接手的工单。</p>
     *
     * @param identity 当前登录身份
     * @param ticketId 工单ID
     * @return 工单详情
     */
    @Override
    public TicketDetailVO getManageTicketDetail(CurrentLoginIdentity identity, Long ticketId) {
        validateCurrentLoginIdentity(identity);
        BizAssert.notNull(ticketId, ErrorCode.PARAM_INVALID, TicketErrorMessages.TICKET_ID_REQUIRED);

        Ticket ticket = this.getById(ticketId);

        BizAssert.notNull(
                ticket,
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.TICKET_NOT_FOUND_OR_NO_PERMISSION
        );

        validateManageDetailPermission(identity, ticket);

        return ticketAssembler.toTicketDetailVO(ticket);
    }


    /**
     * 分页查询处理侧工单列表。
     *
     * @param identity 当前登录身份
     * @param query 查询条件
     * @return 处理侧工单分页结果
     */
    @Override
    public PageResult<TicketManageListItemVO> pageManageTickets(CurrentLoginIdentity identity, TicketManageQuery query) {
        validateCurrentLoginIdentity(identity);
        BizAssert.notNull(query, ErrorCode.PARAM_INVALID, TicketErrorMessages.QUERY_REQUIRED);

        int pageNum = query.safePageNum();
        int pageSize = query.safePageSize();

        LambdaQueryWrapper<Ticket> qw = buildManageTicketQueryWrapper(identity, query);
        qw.orderByDesc(Ticket::getUpdatedAt)
                .orderByDesc(Ticket::getCreatedAt);

        Page<Ticket> page = this.page(new Page<>(pageNum, pageSize), qw);

        List<TicketManageListItemVO> records = page.getRecords().stream()
                .map(ticketAssembler::toTicketManageListItemVO)
                .toList();

        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
    }

    /**
     * 校验处理侧工单详情查看权限。
     *
     * <p>管理员可以查看全部；
     * 支持人员只能查看未分派工单或自己已经接手的工单。</p>
     *
     * @param identity 当前登录身份
     * @param ticket 工单实体
     */
    private void validateManageDetailPermission(CurrentLoginIdentity identity, Ticket ticket) {
        List<String> roleCodes = identity.roleCodes();

        boolean isAdmin = hasRole(roleCodes, PlatformRoleConstants.ADMIN);
        boolean isSupport = hasRole(roleCodes, PlatformRoleConstants.SUPPORT);

        if (isAdmin) {
            return;
        }

        boolean isUnassigned = ticket.getAssigneeId() == null;
        boolean isAssignedToMe = identity.userId() != null
                && identity.userId().equals(ticket.getAssigneeId());

        BizAssert.isTrue(
                isSupport && (isUnassigned || isAssignedToMe),
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.TICKET_NOT_FOUND_OR_NO_PERMISSION
        );
    }


    /**
     * 构建处理侧工单分页查询条件。
     *
     * @param identity 当前登录身份
     * @param query 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapper<Ticket> buildManageTicketQueryWrapper(CurrentLoginIdentity identity,
                                                                     TicketManageQuery query) {
        LambdaQueryWrapper<Ticket> qw = new LambdaQueryWrapper<>();
        applyManagePermissionScope(qw, identity, query);
        applyManageQueryFilters(qw, query);
        return qw;
    }

    /**
     * 应用处理侧数据权限范围。
     *
     * @param qw 查询条件构造器
     * @param identity 当前登录身份
     * @param query 查询条件
     */
    private void applyManagePermissionScope(LambdaQueryWrapper<Ticket> qw,
                                            CurrentLoginIdentity identity,
                                            TicketManageQuery query) {
        Long currentUserId = identity.userId();
        List<String> roleCodes = identity.roleCodes();

        boolean isAdmin = hasRole(roleCodes, PlatformRoleConstants.ADMIN);
        boolean isSupport = hasRole(roleCodes, PlatformRoleConstants.SUPPORT);

        BizAssert.isTrue(
                isAdmin || isSupport,
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.MANAGE_PERMISSION_DENIED
        );

        if (isAdmin) {
            return;
        }

        if (query.mineOnlyOrFalse()) {
            qw.eq(Ticket::getAssigneeId, currentUserId);
            return;
        }

        if (query.unassignedOnlyOrFalse()) {
            qw.isNull(Ticket::getAssigneeId);
            return;
        }

        qw.and(wrapper -> wrapper.eq(Ticket::getAssigneeId, currentUserId)
                .or()
                .isNull(Ticket::getAssigneeId));
    }

    /**
     * 应用处理侧列表筛选条件。
     *
     * @param qw 查询条件构造器
     * @param query 查询条件
     */
    private void applyManageQueryFilters(LambdaQueryWrapper<Ticket> qw, TicketManageQuery query) {
        if (StringUtils.hasText(query.keyword())) {
            String keyword = query.keyword().trim();
            qw.and(wrapper -> wrapper.like(Ticket::getTicketNo, keyword)
                    .or()
                    .like(Ticket::getTitle, keyword));
        }

        if (StringUtils.hasText(query.status())) {
            qw.eq(Ticket::getStatus, query.status().trim());
        }

        if (StringUtils.hasText(query.ticketTypeCode())) {
            qw.eq(Ticket::getTicketTypeCode, query.ticketTypeCode().trim());
        }

        if (StringUtils.hasText(query.source())) {
            qw.eq(Ticket::getSource, query.source().trim());
        }

        if (query.creatorId() != null) {
            qw.eq(Ticket::getCreatorId, query.creatorId());
        }

        if (query.assigneeId() != null) {
            qw.eq(Ticket::getAssigneeId, query.assigneeId());
        }
    }

    /**
     * 判断当前角色列表中是否包含指定角色。
     *
     * @param roleCodes 角色编码列表
     * @param targetRole 目标角色
     * @return true 表示包含目标角色
     */
    private boolean hasRole(List<String> roleCodes, String targetRole) {
        if (CollectionUtils.isEmpty(roleCodes) || !StringUtils.hasText(targetRole)) {
            return false;
        }
        return roleCodes.contains(targetRole);
    }

    /**
     * 校验当前登录身份
     *
     * @param identity 当前登录身份
     * */
    private void validateCurrentLoginIdentity(CurrentLoginIdentity identity) {
        BizAssert.notNull(identity, ErrorCode.PARAM_INVALID, TicketErrorMessages.CURRENT_LOGIN_IDENTITY_REQUIRED);
    }

    /**
     * 接单。
     *
     * <p>接单是处理侧的“唯一占用动作”，
     * 因此这里必须考虑并发下多人同时接同一张工单的问题。</p>
     *
     * <p>当前阶段采用数据库条件更新做乐观式并发控制：</p>
     * <ul>
     *     <li>只有当工单仍然是 PENDING 且 assignee 为空时才允许更新</li>
     *     <li>若更新影响行数为 0，说明工单已被他人接走或状态已变化</li>
     * </ul>
     *
     * <p>这样做可以避免先查后改带来的并发覆盖问题，
     * 同时不需要在第一版过早引入分布式锁。</p>
     *
     * @param identity 当前登录身份
     * @param ticketId 工单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void acceptTicket(CurrentLoginIdentity identity, Long ticketId) {
        validateCurrentLoginIdentity(identity);
        BizAssert.notNull(ticketId, ErrorCode.PARAM_INVALID, TicketErrorMessages.TICKET_ID_REQUIRED);

        validateAcceptPermission(identity);

        Ticket ticket = getById(ticketId);
        BizAssert.notNull(ticket, ErrorCode.BIZ_ERROR, TicketErrorMessages.TICKET_NOT_FOUND);

        validateAcceptPreCheck(ticket);

        int rows = baseMapper.acceptTicket(
                ticketId,
                identity.userId(),
                safeTrim(identity.realName()),
                TicketStatusConstants.PENDING,
                TicketStatusConstants.PROCESSING
        );

        DbAssert.affectedOne(rows, TicketErrorMessages.TICKET_ALREADY_ACCEPTED_OR_STATUS_CHANGED);

        ticketEventService.recordAcceptEvent(ticket, identity.userId(), safeTrim(identity.realName()));

        searchSyncTaskService.enqueueTicketUpsert(ticketId);
    }

    /**
     * 校验接单权限。
     *
     * <p>虽然 Controller 已经通过角色注解做了接口准入控制，
     * 这里仍保留一次 Service 层兜底校验，
     * 防止后续该方法被其他内部入口复用时出现权限缺口。</p>
     *
     * @param identity 当前登录身份
     */
    private void validateAcceptPermission(CurrentLoginIdentity identity) {
        List<String> roleCodes = identity.roleCodes();
        boolean isAdmin = hasRole(roleCodes, PlatformRoleConstants.ADMIN);
        boolean isSupport = hasRole(roleCodes, PlatformRoleConstants.SUPPORT);

        BizAssert.isTrue(
                isAdmin || isSupport,
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.ACCEPT_PERMISSION_DENIED
        );
    }

    /**
     * 接单前置校验。
     *
     * <p>这里先做一次业务友好的前置判断，
     * 让调用方在明显不满足条件时拿到更清晰的提示。</p>
     *
     * <p>校验顺序上优先判断“是否已被分派”，
     * 因为对接单场景来说，用户最关心的是这张工单是否已经被别人接走；
     * 若仍未分派，再继续判断当前状态是否允许接单。</p>
     *
     * <p>真正的并发安全仍依赖后续数据库条件更新，
     * 因此这里的前置校验不是并发控制本身，
     * 而是为了提升接口语义和错误提示的可读性。</p>
     *
     * @param ticket 工单实体
     */
    private void validateAcceptPreCheck(Ticket ticket) {
        BizAssert.isNull(
                ticket.getAssigneeId(),
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.TICKET_ALREADY_ASSIGNED
        );

        BizAssert.isTrue(
                TicketStatusMachine.canTransfer(ticket.getStatus(), TicketStatusConstants.PROCESSING),
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.TICKET_STATUS_NOT_ALLOW_ACCEPT
        );
    }


    /**
     * 更新工单状态。
     *
     * <p>当前阶段该方法只负责将“处理中”的工单推进到最终处理结果，
     * 即更新为已解决或已拒绝。</p>
     *
     * <p>状态更新前需要完成三类校验：</p>
     * <ul>
     *     <li>参数校验：目标状态与状态说明是否合法</li>
     *     <li>权限校验：当前用户是否为管理员或当前接单人</li>
     *     <li>流转校验：当前工单是否处于允许更新的状态</li>
     * </ul>
     *
     * <p>最终落库采用数据库条件更新，
     * 避免并发下发生状态覆盖问题。</p>
     *
     * @param identity 当前登录身份
     * @param ticketId 工单ID
     * @param request 更新状态请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTicketStatus(CurrentLoginIdentity identity, Long ticketId, UpdateTicketStatusRequest request) {
        validateCurrentLoginIdentity(identity);
        BizAssert.notNull(ticketId, ErrorCode.PARAM_INVALID, TicketErrorMessages.TICKET_ID_REQUIRED);
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, TicketErrorMessages.QUERY_REQUIRED);

        validateUpdateTicketStatusRequest(request);

        Ticket ticket = getById(ticketId);
        BizAssert.notNull(ticket, ErrorCode.BIZ_ERROR, TicketErrorMessages.TICKET_NOT_FOUND);

        validateUpdateStatusPermission(identity, ticket);
        validateStatusTransition(ticket, request.targetStatus());

        String targetStatus = safeTrim(request.targetStatus());
        String statusRemark = safeTrim(request.statusRemark());

        int rows = baseMapper.updateTicketStatus(
                ticketId,
                ticket.getAssigneeId(),
                ticket.getStatus(),
                targetStatus,
                statusRemark
        );

        DbAssert.affectedOne(rows, TicketErrorMessages.TICKET_STATUS_UPDATE_CONFLICT);

        ticketEventService.recordStatusChangeEvent(
                ticket,
                identity.userId(),
                safeTrim(identity.realName()),
                targetStatus,
                statusRemark
        );

        searchSyncTaskService.enqueueTicketUpsert(ticketId);
    }

    /**
     * 校验更新工单状态请求。
     *
     * <p>当前阶段状态更新接口只允许将工单更新为已解决或已拒绝，
     * 并要求必须同时填写状态说明，
     * 用于向员工侧展示处理结果或拒绝原因。</p>
     *
     * @param request 更新状态请求
     */
    private void validateUpdateTicketStatusRequest(UpdateTicketStatusRequest request) {
        BizAssert.hasText(
                request.targetStatus(),
                ErrorCode.PARAM_INVALID,
                TicketErrorMessages.STATUS_TARGET_REQUIRED
        );
        BizAssert.hasText(
                request.statusRemark(),
                ErrorCode.PARAM_INVALID,
                TicketErrorMessages.STATUS_REMARK_REQUIRED
        );

        String targetStatus = safeTrim(request.targetStatus());
        BizAssert.isTrue(
                TicketStatusMachine.isAllowedProcessResultStatus(targetStatus),
                ErrorCode.PARAM_INVALID,
                TicketErrorMessages.STATUS_TARGET_INVALID
        );
    }

    /**
     * 校验更新工单状态权限。
     *
     * <p>当前阶段允许两类用户更新工单状态：</p>
     * <ul>
     *     <li>管理员</li>
     *     <li>当前工单接单人</li>
     * </ul>
     *
     * <p>这样做的目的是既保证处理人可以推进自己负责工单的状态，
     * 也保留管理员在特殊场景下的兜底处理能力。</p>
     *
     * @param identity 当前登录身份
     * @param ticket 工单实体
     */
    private void validateUpdateStatusPermission(CurrentLoginIdentity identity, Ticket ticket) {
        boolean isAdmin = hasRole(identity.roleCodes(), PlatformRoleConstants.ADMIN);
        boolean isAssignee = identity.userId() != null && identity.userId().equals(ticket.getAssigneeId());

        BizAssert.isTrue(
                isAdmin || isAssignee,
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.STATUS_UPDATE_PERMISSION_DENIED
        );
    }

    /**
     * 校验状态流转是否合法。
     *
     * <p>当前阶段状态更新接口只负责处理“处理中”工单的最终结果推进，
     * 因此只有处于 PROCESSING 状态的工单才允许通过该接口更新状态。</p>
     *
     * <p>接单动作负责将工单从 PENDING 推进到 PROCESSING，
     * 不允许通过该接口直接替代接单动作。</p>
     *
     * @param ticket 工单实体
     * @param targetStatus 目标状态
     */
    private void validateStatusTransition(Ticket ticket, String targetStatus) {
        String currentStatus = safeTrim(ticket.getStatus());
        String target = safeTrim(targetStatus);

        BizAssert.isTrue(
                TicketStatusMachine.canTransfer(currentStatus, target),
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.TICKET_STATUS_NOT_ALLOW_UPDATE
        );
    }

    /**
     * 校验工单分配请求。
     *
     * <p>当前阶段工单分配接口只允许将工单分配给指定处理人，
     * 并要求工单当前状态为“待处理”。</p>
     *
     * @param request 工单分配请求
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTicket(CurrentLoginIdentity identity, Long ticketId, AssignTicketRequest request) {
        validateCurrentLoginIdentity(identity);
        BizAssert.notNull(ticketId, ErrorCode.PARAM_INVALID, TicketErrorMessages.TICKET_ID_REQUIRED);
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, TicketErrorMessages.ASSIGN_REQUEST_REQUIRED);

        validateAssignTicketRequest(request);
        validateAssignPermission(identity);

        InternalUserInfoResponse assignee = resolveAssignableAssignee(request.assigneeId());

        Ticket ticket = getById(ticketId);
        BizAssert.notNull(ticket, ErrorCode.BIZ_ERROR, TicketErrorMessages.TICKET_NOT_FOUND);

        validateAssignPreCheck(ticket);

        String assigneeName = safeTrim(assignee.realName());

        int rows = baseMapper.assignTicket(
                ticketId,
                assignee.userId(),
                assigneeName,
                TicketStatusConstants.PENDING,
                TicketStatusConstants.PROCESSING
        );

        DbAssert.affectedOne(rows, TicketErrorMessages.TICKET_ALREADY_ASSIGNED_OR_STATUS_CHANGED);

        ticketEventService.recordAssignEvent(
                ticket,
                identity.userId(),
                safeTrim(identity.realName()),
                assignee.userId(),
                assigneeName
        );

        searchSyncTaskService.enqueueTicketUpsert(ticketId);
    }

    /**
     * 校验工单分配请求。
     *
     * <p>当前阶段工单分配接口只允许将工单分配给指定处理人，
     * 并要求工单当前状态为“待处理”。</p>
     *
     * @param request 工单分配请求
     */
    private void validateAssignTicketRequest(AssignTicketRequest request) {
        BizAssert.notNull(
                request.assigneeId(),
                ErrorCode.PARAM_INVALID,
                TicketErrorMessages.ASSIGNEE_ID_REQUIRED
        );
    }

    /**
     * 查询并校验可被分派的处理人。
     *
     * <p>处理人必须满足三个条件：
     * 一是用户存在；
     * 二是用户处于启用状态；
     * 三是具备 SUPPORT 或 ADMIN 角色。</p>
     *
     * @param assigneeId 处理人ID
     * @return 处理人内部用户信息
     */
    private InternalUserInfoResponse resolveAssignableAssignee(Long assigneeId) {
        Result<InternalUserInfoResponse> result;

        try {
            result = authUserClient.getUserById(assigneeId);
        } catch (Exception ex) {
            throw new com.xcvk.platform.common.exception.BusinessException(
                    ErrorCode.SERVICE_UNAVAILABLE,
                    TicketErrorMessages.ASSIGNEE_QUERY_FAILED
            );
        }

        BizAssert.notNull(
                result,
                ErrorCode.SERVICE_UNAVAILABLE,
                TicketErrorMessages.ASSIGNEE_QUERY_FAILED
        );

        BizAssert.isTrue(
                result.getCode() == ErrorCode.SUCCESS.getCode() && result.getData() != null,
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.ASSIGNEE_NOT_FOUND_OR_DISABLED
        );

        InternalUserInfoResponse assignee = result.getData();

        boolean canHandleTicket = hasRole(assignee.roleCodes(), PlatformRoleConstants.SUPPORT)
                || hasRole(assignee.roleCodes(), PlatformRoleConstants.ADMIN);

        BizAssert.isTrue(
                canHandleTicket,
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.ASSIGNEE_ROLE_INVALID
        );

        BizAssert.hasText(
                assignee.realName(),
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.ASSIGNEE_NOT_FOUND_OR_DISABLED
        );

        return assignee;
    }

    /**
     * 校验工单分配权限。
     *
     * <p>当前阶段工单分配接口只允许管理员分配工单。</p>
     *
     * @param identity 当前登录身份
     */
    private void validateAssignPermission(CurrentLoginIdentity identity) {
        BizAssert.isTrue(
                hasRole(identity.roleCodes(), PlatformRoleConstants.ADMIN),
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.ASSIGN_PERMISSION_DENIED
        );
    }

    /**
     * 校验工单分配前置条件。
     *
     * <p>当前阶段工单分配接口只允许将工单分配给指定处理人，
     * 并要求工单当前状态为“待处理”。</p>
     *
     * @param ticket 工单实体
     */
    private void validateAssignPreCheck(Ticket ticket) {
        BizAssert.isNull(
                ticket.getAssigneeId(),
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.TICKET_ALREADY_ASSIGNED
        );

        BizAssert.isTrue(
                TicketStatusMachine.canTransfer(ticket.getStatus(), TicketStatusConstants.PROCESSING),
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.TICKET_STATUS_NOT_ALLOW_ASSIGN
        );
    }

    /**
     * AI Agent 创建工单。
     *
     * <p>AI 创建工单与手工创建工单共享同一套主流程，
     * 区别在于来源固定为 AI_AGENT，并写入 sourceRef 用于追踪 Agent 执行链路。</p>
     *
     * @param creatorId 创建人ID
     * @param creatorName 创建人姓名
     * @param ticketTypeCode 工单类型编码
     * @param title 工单标题
     * @param content 工单内容
     * @param priority 优先级
     * @param sourceRef AI会话ID / Agent执行ID
     * @return 工单创建结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateTicketResponse createAiTicket(Long creatorId, String creatorName,
                                               String ticketTypeCode, String title,
                                               String content, String priority,
                                               String sourceRef) {
        CreateTicketCmd cmd = new CreateTicketCmd(
                creatorId,
                creatorName,
                ticketTypeCode,
                title,
                content,
                priority,
                TicketSourceConstants.AI_AGENT,
                sourceRef
        );

        validateCreateCmd(cmd);
        TicketType ticketType = ticketTypeService.getEnabledTicketType(cmd.ticketTypeCode());
        validateTicketSource(cmd, ticketType);

        Long ticketId = idGenerator.nextId();
        String ticketNo = buildTicketNo(ticketId);

        Ticket ticket = buildTicket(cmd, ticketType, ticketId, ticketNo);

        int rows = baseMapper.insert(ticket);
        DbAssert.affectedOne(rows, TicketErrorMessages.CREATE_FAILED);

        ticketEventService.recordCreateEvent(ticket, creatorId, creatorName);

        searchSyncTaskService.enqueueTicketUpsert(ticketId);

        return new CreateTicketResponse(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getStatus()
        );
    }

    @Override
    public QueryAiTicketResponse queryTicketByAi(QueryAiTicketRequest request) {
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, "查询工单请求不能为空");

        Long creatorId = request.creatorId();
        Long assigneeId = request.assigneeId();

        BizAssert.isTrue(
                creatorId != null || assigneeId != null,
                ErrorCode.PARAM_INVALID,
                "查询工单用户范围不能为空"
        );

        boolean detailQuery = StringUtils.hasText(request.ticketNo());

        LambdaQueryWrapper<Ticket> qw = Wrappers.lambdaQuery(Ticket.class);
        applyAiTicketScope(qw, request);

        if (StringUtils.hasText(request.ticketNo())) {
            qw.eq(Ticket::getTicketNo, request.ticketNo().trim());
        }

        if (StringUtils.hasText(request.status())) {
            qw.eq(Ticket::getStatus, request.status().trim());
        }

        qw.orderByDesc(Ticket::getCreatedAt);

        int pageNum = request.pageNum() == null || request.pageNum() <= 0
                ? DEFAULT_PAGE_NUM
                : request.pageNum();

        int pageSize = request.pageSize() == null || request.pageSize() <= 0
                ? DEFAULT_PAGE_SIZE
                : request.pageSize();

        if (detailQuery) {
            pageNum = 1;
            pageSize = 1;
        }

        Page<Ticket> page = new Page<>(pageNum, pageSize);

        Page<Ticket> ticketPage = baseMapper.selectPage(page, qw);

        List<QueryAiTicketItem> items = ticketPage.getRecords()
                .stream()
                .map(this::toQueryAiTicketItem)
                .toList();

        return new QueryAiTicketResponse(
                detailQuery ? "DETAIL" : "LIST",
                Math.toIntExact(ticketPage.getTotal()),
                items
        );
    }

    private void applyAiTicketScope(LambdaQueryWrapper<Ticket> qw, QueryAiTicketRequest request) {
        String scope = StringUtils.hasText(request.scope())
                ? request.scope().trim()
                : "CREATED_BY_ME";

        Long creatorId = request.creatorId();
        Long assigneeId = request.assigneeId();

        switch (scope) {
            case "ASSIGNED_TO_ME" -> {
                BizAssert.notNull(assigneeId, ErrorCode.PARAM_INVALID, "处理人ID不能为空");
                qw.eq(Ticket::getAssigneeId, assigneeId);
            }
            case "RELATED_TO_ME" -> {
                BizAssert.isTrue(
                        creatorId != null || assigneeId != null,
                        ErrorCode.PARAM_INVALID,
                        "相关工单查询范围不能为空"
                );

                qw.and(wrapper -> {
                    boolean hasCondition = false;

                    if (creatorId != null) {
                        wrapper.eq(Ticket::getCreatorId, creatorId);
                        hasCondition = true;
                    }

                    if (assigneeId != null) {
                        if (hasCondition) {
                            wrapper.or();
                        }
                        wrapper.eq(Ticket::getAssigneeId, assigneeId);
                    }
                });
            }
            case "CREATED_BY_ME" -> {
                BizAssert.notNull(creatorId, ErrorCode.PARAM_INVALID, TicketErrorMessages.CREATOR_ID_REQUIRED);
                qw.eq(Ticket::getCreatorId, creatorId);
            }
            default -> {
                BizAssert.notNull(creatorId, ErrorCode.PARAM_INVALID, TicketErrorMessages.CREATOR_ID_REQUIRED);
                qw.eq(Ticket::getCreatorId, creatorId);
            }
        }
    }

    private QueryAiTicketItem toQueryAiTicketItem(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        return new QueryAiTicketItem(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getTicketTypeId(),
                ticket.getTicketTypeCode(),
                ticket.getTicketTypeName(),
                ticket.getTitle(),
                ticket.getContent(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getSource(),
                ticket.getSourceRef(),
                ticket.getCreatorId(),
                ticket.getCreatorName(),
                ticket.getAssigneeId(),
                ticket.getAssigneeName(),
                ticket.getClosedAt(),
                ticket.getStatusRemark(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    @Override
    public List<TicketEventVO> listTicketEvents(CurrentLoginIdentity identity, Long ticketId) {
        validateCurrentLoginIdentity(identity);
        BizAssert.notNull(ticketId, ErrorCode.PARAM_INVALID, TicketErrorMessages.TICKET_ID_REQUIRED);

        Ticket ticket = getById(ticketId);
        BizAssert.notNull(ticket, ErrorCode.BIZ_ERROR, TicketErrorMessages.TICKET_NOT_FOUND);

        validateTicketEventViewPermission(identity, ticket);

        return ticketEventService.listTicketEvents(ticketId);
    }

    /**
     * 校验工单操作流水查看权限。
     *
     * <p>当前最小闭环版允许以下用户查看：</p>
     * <ul>
     *     <li>管理员</li>
     *     <li>工单创建人</li>
     *     <li>当前工单处理人</li>
     * </ul>
     */
    private void validateTicketEventViewPermission(CurrentLoginIdentity identity, Ticket ticket) {
        boolean isAdmin = hasRole(identity.roleCodes(), PlatformRoleConstants.ADMIN);
        boolean isSupport = hasRole(identity.roleCodes(), PlatformRoleConstants.SUPPORT);
        boolean isCreator = identity.userId() != null && identity.userId().equals(ticket.getCreatorId());
        boolean isAssignee = identity.userId() != null && identity.userId().equals(ticket.getAssigneeId());
        boolean isUnassigned = ticket.getAssigneeId() == null;

        BizAssert.isTrue(
                isAdmin || isCreator || isAssignee || (isSupport && isUnassigned),
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.TICKET_EVENT_PERMISSION_DENIED
        );
    }

}