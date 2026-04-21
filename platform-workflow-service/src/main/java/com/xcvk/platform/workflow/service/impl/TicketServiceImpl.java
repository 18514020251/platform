package com.xcvk.platform.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcvk.platform.auth.starter.constant.PlatformRoleConstants;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.common.enums.CommonStatusEnum;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.common.util.DbAssert;
import com.xcvk.platform.id.generator.SnowflakeIdGenerator;
import com.xcvk.platform.workflow.constant.TicketErrorMessages;
import com.xcvk.platform.workflow.constant.TicketSourceConstants;
import com.xcvk.platform.workflow.constant.TicketStatusConstants;
import com.xcvk.platform.workflow.model.cmd.CreateTicketCmd;
import com.xcvk.platform.workflow.model.entity.Ticket;
import com.xcvk.platform.workflow.model.entity.TicketType;
import com.xcvk.platform.workflow.model.query.MyTicketQuery;
import com.xcvk.platform.workflow.model.query.TicketManageQuery;
import com.xcvk.platform.workflow.model.vo.CreateTicketResponse;
import com.xcvk.platform.workflow.model.vo.TicketDetailVO;
import com.xcvk.platform.workflow.model.vo.TicketListItemVO;
import com.xcvk.platform.workflow.model.vo.TicketManageListItemVO;
import com.xcvk.platform.workflow.repository.mapper.TicketMapper;
import com.xcvk.platform.workflow.service.TicketService;
import com.xcvk.platform.workflow.service.TicketTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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
@RequiredArgsConstructor
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements TicketService {

    private static final String DEFAULT_PRIORITY = "MEDIUM";

    private final TicketTypeService ticketTypeService;
    private final SnowflakeIdGenerator idGenerator;

    /**
     * 创建工单主流程。
     *
     * @param cmd 创建工单命令对象
     * @return 创建结果
     */
    @Override
    public CreateTicketResponse createTicket(CreateTicketCmd cmd) {
        validateCreateCmd(cmd);

        TicketType ticketType = ticketTypeService.getEnabledTicketType(cmd.ticketTypeCode());
        validateTicketSource(cmd, ticketType);

        Long ticketId = idGenerator.nextId();
        String ticketNo = buildTicketNo(ticketId);

        Ticket ticket = buildTicket(cmd, ticketType, ticketId, ticketNo);

        int rows = baseMapper.insert(ticket);
        DbAssert.affectedOne(rows, TicketErrorMessages.CREATE_FAILED);

        return new CreateTicketResponse(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getStatus()
        );
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
                .map(this::toTicketListItemVO)
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
     * 将工单实体转换为员工侧列表项视图对象。
     *
     * @param ticket 工单实体
     * @return 工单列表项
     */
    private TicketListItemVO toTicketListItemVO(Ticket ticket) {
        return new TicketListItemVO(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getTicketTypeCode(),
                ticket.getTicketTypeName(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getSource(),
                ticket.getAssigneeName(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    /**
     * 查询当前登录用户自己的工单详情。
     *
     * @param creatorId 创建人ID
     * @param ticketId 工单ID
     * @return 工单详情
     */
    @Override
    public TicketDetailVO getMyTicketDetail(Long creatorId, Long ticketId) {
        BizAssert.notNull(creatorId, ErrorCode.PARAM_INVALID, TicketErrorMessages.CREATOR_ID_REQUIRED);
        BizAssert.notNull(ticketId, ErrorCode.PARAM_INVALID, TicketErrorMessages.TICKET_ID_REQUIRED);

        Ticket ticket = this.getOne(new LambdaQueryWrapper<Ticket>()
                .eq(Ticket::getId, ticketId)
                .eq(Ticket::getCreatorId, creatorId)
        );

        BizAssert.notNull(ticket, ErrorCode.BIZ_ERROR, TicketErrorMessages.TICKET_NOT_FOUND_OR_NO_PERMISSION);
        return buildTicketDetailVO(ticket);
    }

    /**
     * 将工单实体转换为详情视图对象。
     *
     * @param ticket 工单实体
     * @return 工单详情
     */
    private TicketDetailVO buildTicketDetailVO(Ticket ticket) {
        return new TicketDetailVO(
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
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
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
        BizAssert.notNull(identity, ErrorCode.PARAM_INVALID, "当前登录身份不能为空");
        BizAssert.notNull(query, ErrorCode.PARAM_INVALID, TicketErrorMessages.QUERY_REQUIRED);

        int pageNum = query.safePageNum();
        int pageSize = query.safePageSize();

        LambdaQueryWrapper<Ticket> qw = buildManageTicketQueryWrapper(identity, query);
        qw.orderByDesc(Ticket::getUpdatedAt)
                .orderByDesc(Ticket::getCreatedAt);

        Page<Ticket> page = this.page(new Page<>(pageNum, pageSize), qw);

        List<TicketManageListItemVO> records = page.getRecords().stream()
                .map(this::toTicketManageListItemVO)
                .toList();

        return PageResult.of(records, page.getTotal(), pageNum, pageSize);
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

        // Controller 层已经通过角色注解做了接口准入控制，
        // 这里继续保留一次角色判断，作为 Service 层兜底保护，
        // 避免后续该方法被其他入口复用时出现权限缺口。
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
     * 将工单实体转换为处理侧列表项视图对象。
     *
     * @param ticket 工单实体
     * @return 处理侧工单列表项
     */
    private TicketManageListItemVO toTicketManageListItemVO(Ticket ticket) {
        return new TicketManageListItemVO(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getTicketTypeCode(),
                ticket.getTicketTypeName(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getSource(),
                ticket.getCreatorId(),
                ticket.getCreatorName(),
                ticket.getAssigneeId(),
                ticket.getAssigneeName(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
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
    public void acceptTicket(CurrentLoginIdentity identity, Long ticketId) {
        BizAssert.notNull(identity, ErrorCode.PARAM_INVALID, "当前登录身份不能为空");
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


        DbAssert.affectedOne(rows,TicketErrorMessages.TICKET_ALREADY_ACCEPTED_OR_STATUS_CHANGED);
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
                TicketStatusConstants.PENDING.equals(ticket.getStatus()),
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.TICKET_STATUS_NOT_ALLOW_ACCEPT
        );
    }
}