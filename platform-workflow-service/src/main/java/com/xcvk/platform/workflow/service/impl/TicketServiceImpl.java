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
 *     <li>处理侧：支持人员/管理员视角的工单列表</li>
 * </ul>
 *
 * <p>实现上优先遵循“主链优先、低风险、先闭环再增强”的原则。
 * 因此当前阶段处理侧列表先基于现有工单主表能力实现，
 * 不提前引入过重的联表权限模型。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-21
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
     * <p>该方法是 workflow 域内统一的工单创建入口，
     * 目的是让手工创建与 AI 创建共享同一套业务规则，
     * 避免不同入口各自落一套工单创建逻辑，后续难以维护。</p>
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
     * <p>这里先统一做命令对象级别的基础校验，
     * 可以让后续主流程只聚焦业务判断，
     * 避免参数校验散落在创建流程各个位置。</p>
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
     * <p>当前阶段采用“TK + 日期 + 6位尾号”的轻量规则，
     * 优先满足可读性和演示友好性，
     * 同时避免为了编号规则额外引入复杂基础设施。</p>
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
     * <p>这样做的目的是把“来源合法性”和“来源对应的业务边界”放在一起判断，
     * 避免 AI 创建规则散落在其他流程节点中。</p>
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
     * <p>优先级采用“显式传入优先于类型默认值，再回落系统默认值”的顺序，
     * 目的是既保留调用方指定优先级的灵活性，
     * 也让工单类型配置具备默认语义。</p>
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
     * <p>工单主表保留工单类型快照，
     * 是为了避免后续工单类型名称发生调整时影响历史工单的业务语义展示。</p>
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
     * <p>该方法只服务员工侧“我的工单”场景，
     * 查询范围严格限定为当前用户自己创建的工单，
     * 避免员工侧列表被误用为处理侧查询入口。</p>
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
     * <p>当前阶段员工侧只支持按创建人和工单状态筛选，
     * 保持员工主链查询能力简洁稳定。</p>
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
     * <p>该方法通过“工单ID + 创建人ID”双条件限制查询范围，
     * 保证员工只能查看自己的工单详情。</p>
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
     * <p>当前阶段详情只返回工单主表中的当前态信息，
     * 处理记录、操作轨迹等历史类数据后续再由 MongoDB 链路承接。</p>
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
     * <p>该方法是处理工作台的第一个核心入口，
     * 后续接单、分派、状态流转等接口都将围绕这批可见工单展开。</p>
     *
     * <p>当前阶段采用保守实现：</p>
     * <ul>
     *     <li>管理员查看全部工单</li>
     *     <li>支持人员查看分配给自己或未分派的工单</li>
     * </ul>
     *
     * <p>这样做是为了优先打通处理侧闭环，
     * 后续如需基于工单类型默认处理角色做更精细的数据权限控制，
     * 再逐步增强，不在第一版过早做重。</p>
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
     * <p>处理侧查询由两部分组成：</p>
     * <ul>
     *     <li>先按当前登录身份施加数据权限范围</li>
     *     <li>再叠加查询参数做筛选</li>
     * </ul>
     *
     * <p>这样拆分的目的是把“能看什么”和“想筛什么”两个维度分开，
     * 后续维护权限逻辑时更清晰。</p>
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
     * <p>当前阶段采用最小可用权限策略：</p>
     * <ul>
     *     <li>管理员：不加范围限制，默认可看全部</li>
     *     <li>支持人员：默认可看“我的工单 + 未分派工单”</li>
     * </ul>
     *
     * <p>若显式传入 mineOnly / unassignedOnly，则收敛到对应子集，
     * 便于处理工作台快速切换“我的待处理”和“待认领池”。</p>
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
     * <p>当前阶段筛选条件尽量保持轻量，
     * 只覆盖处理工作台最常用的筛选维度，
     * 避免第一个处理侧接口就过早走向复杂查询模型。</p>
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
     * <p>这里单独抽取角色判断，是为了避免角色匹配逻辑在多个地方重复出现，
     * 后续若角色来源结构调整，只需在一处维护。</p>
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
}