package com.xcvk.platform.workflow.service.impl;

import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.common.enums.CommonStatusEnum;
import com.xcvk.platform.common.exception.BusinessException;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.id.generator.SnowflakeIdGenerator;
import com.xcvk.platform.workflow.constant.TicketSourceConstants;
import com.xcvk.platform.workflow.constant.TicketStatusConstants;
import com.xcvk.platform.workflow.model.cmd.CreateTicketCmd;
import com.xcvk.platform.workflow.model.entity.Ticket;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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


/**
 * <p>
 * 工单主表 服务实现类
 * </p>
 *
 * @author Programmer
 * @since 2026-04-20
 */
@Service
@RequiredArgsConstructor
public class TicketServiceImpl extends ServiceImpl<TicketMapper, Ticket> implements TicketService {

    private static final String DEFAULT_TICKET_NO_PREFIX = "TK-";
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
        if (rows != 1) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "工单创建失败");
        }

        return new CreateTicketResponse(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getStatus()
        );
    }


    private void validateCreateCmd(CreateTicketCmd cmd) {
        if (cmd == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID);
        }

        validateRequired(cmd.ticketTypeCode(), "工单类型");
        validateRequired(cmd.title(), "工单标题");
        validateRequired(cmd.content(), "工单内容");
        validateRequired(cmd.creatorName(), "创建人");
        validateRequired(cmd.creatorId(), "创建人ID");
    }

    private void validateRequired(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, fieldName + "不能为空");
        }
    }

    private void validateRequired(Object value, String fieldName) {
        if (value == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, fieldName + "不能为空");
        }
    }

    /**
     * 构建工单编号
     *
     * @param ticketId 工单ID
     * @return 工单编号
     * */
    private String buildTicketNo(Long ticketId) {
        return DEFAULT_TICKET_NO_PREFIX.concat(ticketId.toString()) ;
    }

    /**
     * 校验工单来源
     *
     * @param cmd 创建工单命令对象
     * @param ticketType 工单类型对象
     * */
    private void validateTicketSource(CreateTicketCmd cmd, TicketType ticketType) {
        if (!StringUtils.hasText(cmd.source())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "工单来源不能为空");
        }

        if (!TicketSourceConstants.MANUAL.equals(cmd.source())
                && !TicketSourceConstants.AI_AGENT.equals(cmd.source())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "工单来源无效");
        }

        if (TicketSourceConstants.AI_AGENT.equals(cmd.source())
                && !CommonStatusEnum.isEnabled(ticketType.getAllowAiCreate())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "当前工单类型不允许 AI 创建");
        }
    }

    /**
     * 构建工单实体
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

    private String safeTrim(String value) {
        return value != null ? value.trim() : null;
    }

    private String resolvePriority(String priority, String defaultPriority) {
        if (StringUtils.hasText(priority)) {
            return priority.trim();
        }
        if (StringUtils.hasText(defaultPriority)) {
            return defaultPriority.trim();
        }
        return DEFAULT_PRIORITY;
    }

    @Override
    public PageResult<TicketListItemVO> pageMyTickets(Long creatorId, MyTicketQuery query) {
        return null;
    }

    @Override
    public TicketDetailVO getMyTicketDetail(Long creatorId, Long ticketId) {
        return null;
    }
}
