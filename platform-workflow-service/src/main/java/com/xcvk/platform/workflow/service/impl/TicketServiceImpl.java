package com.xcvk.platform.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
import com.xcvk.platform.workflow.model.vo.CreateTicketResponse;
import com.xcvk.platform.workflow.model.vo.TicketDetailVO;
import com.xcvk.platform.workflow.model.vo.TicketListItemVO;
import com.xcvk.platform.workflow.repository.mapper.TicketMapper;
import com.xcvk.platform.workflow.service.TicketService;
import com.xcvk.platform.workflow.service.TicketTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工单服务实现类
 *
 * <p>当前阶段围绕员工主链提供工单创建、我的工单列表和工单详情能力。</p>
 *
 * @author Programmer
 * @since 2026-04-20
 */
@Service
@RequiredArgsConstructor
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements TicketService {

    private static final String DEFAULT_PRIORITY = "MEDIUM";

    private final TicketTypeService ticketTypeService;
    private final SnowflakeIdGenerator idGenerator;

    /**
     * 创建工单主流程：
     * 先校验命令对象和工单类型，再根据来源规则做业务校验，
     * 最终生成工单编号并落库。
     *
     * <p>手工创建和 AI 创建最终都会收敛到该入口，
     * 以保证工单创建逻辑只有一份实现。</p>
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
        String datePart = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
        String sequencePart = String.format("%06d", Math.abs(ticketId % 1_000_000));
        return "TK" + datePart + sequencePart;
    }

    /**
     * 校验工单来源是否合法，并在 AI 创建场景下校验工单类型是否允许 AI 发起。
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
     * 解析工单优先级：
     * 优先使用显式传入值；若未传，则回落到工单类型默认优先级；仍为空时使用系统默认值。
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

        LambdaQueryWrapper<Ticket> qw = buildTicketQueryWrapper(creatorId, query);
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
     * <p>当前阶段只支持按创建人和工单状态筛选。</p>
     *
     * @param creatorId 创建人ID
     * @param query 查询条件
     * @return 查询条件构造器
     */
    private LambdaQueryWrapper<Ticket> buildTicketQueryWrapper(Long creatorId, MyTicketQuery query) {
        LambdaQueryWrapper<Ticket> qw = new LambdaQueryWrapper<>();
        qw.eq(Ticket::getCreatorId, creatorId);

        if (StringUtils.hasText(query.status())) {
            qw.eq(Ticket::getStatus, query.status().trim());
        }

        return qw;
    }

    /**
     * 将工单实体转换为列表项视图对象。
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
}