package com.xcvk.platform.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.id.generator.SnowflakeIdGenerator;
import com.xcvk.platform.workflow.constant.TicketErrorMessages;
import com.xcvk.platform.workflow.constant.TicketEventTypeConstants;
import com.xcvk.platform.workflow.constant.TicketStatusConstants;
import com.xcvk.platform.workflow.model.entity.Ticket;
import com.xcvk.platform.workflow.model.entity.TicketEvent;
import com.xcvk.platform.workflow.model.vo.TicketEventVO;
import com.xcvk.platform.workflow.repository.mapper.TicketEventMapper;
import com.xcvk.platform.workflow.service.TicketEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工单操作流水服务实现
 */
@Service
@RequiredArgsConstructor
public class TicketEventServiceImpl extends ServiceImpl<TicketEventMapper, TicketEvent>
        implements TicketEventService {

    private static final String SYSTEM_OPERATOR_NAME = "系统";

    private final SnowflakeIdGenerator idGenerator;

    @Override
    public void recordCreateEvent(Ticket ticket, Long operatorId, String operatorName) {
        recordEvent(
                ticket,
                TicketEventTypeConstants.CREATE,
                operatorId,
                operatorName,
                null,
                TicketStatusConstants.PENDING,
                "创建工单"
        );
    }

    @Override
    public void recordAcceptEvent(Ticket ticket, Long operatorId, String operatorName) {
        recordEvent(
                ticket,
                TicketEventTypeConstants.ACCEPT,
                operatorId,
                operatorName,
                TicketStatusConstants.PENDING,
                TicketStatusConstants.PROCESSING,
                "接单"
        );
    }

    @Override
    public void recordAssignEvent(Ticket ticket,
                                  Long operatorId,
                                  String operatorName,
                                  Long assigneeId,
                                  String assigneeName) {
        String remark = "派单给：" + safeText(assigneeName, String.valueOf(assigneeId));

        recordEvent(
                ticket,
                TicketEventTypeConstants.ASSIGN,
                operatorId,
                operatorName,
                TicketStatusConstants.PENDING,
                TicketStatusConstants.PROCESSING,
                remark
        );
    }

    @Override
    public void recordStatusChangeEvent(Ticket ticket,
                                        Long operatorId,
                                        String operatorName,
                                        String targetStatus,
                                        String remark) {
        String eventType = resolveStatusEventType(targetStatus);

        recordEvent(
                ticket,
                eventType,
                operatorId,
                operatorName,
                TicketStatusConstants.PROCESSING,
                targetStatus,
                remark
        );
    }

    @Override
    public List<TicketEventVO> listTicketEvents(Long ticketId) {
        BizAssert.notNull(ticketId, ErrorCode.PARAM_INVALID, TicketErrorMessages.TICKET_ID_REQUIRED);

        return this.list(
                        Wrappers.lambdaQuery(TicketEvent.class)
                                .eq(TicketEvent::getTicketId, ticketId)
                                .orderByAsc(TicketEvent::getCreatedAt)
                                .orderByAsc(TicketEvent::getId)
                )
                .stream()
                .map(this::toVO)
                .toList();
    }

    private void recordEvent(Ticket ticket,
                             String eventType,
                             Long operatorId,
                             String operatorName,
                             String fromStatus,
                             String toStatus,
                             String remark) {
        if (ticket == null || ticket.getId() == null) {
            return;
        }

        TicketEvent event = new TicketEvent()
                .setId(idGenerator.nextId())
                .setTicketId(ticket.getId())
                .setTicketNo(ticket.getTicketNo())
                .setEventType(eventType)
                .setOperatorId(operatorId)
                .setOperatorName(safeText(operatorName, SYSTEM_OPERATOR_NAME))
                .setFromStatus(fromStatus)
                .setToStatus(toStatus)
                .setRemark(trimToNull(remark));

        this.save(event);
    }

    private String resolveStatusEventType(String targetStatus) {
        if (TicketStatusConstants.RESOLVED.equals(targetStatus)) {
            return TicketEventTypeConstants.RESOLVE;
        }

        if (TicketStatusConstants.REJECTED.equals(targetStatus)) {
            return TicketEventTypeConstants.REJECT;
        }

        return "STATUS_CHANGE";
    }

    private TicketEventVO toVO(TicketEvent event) {
        return new TicketEventVO(
                event.getId(),
                event.getTicketId(),
                event.getTicketNo(),
                event.getEventType(),
                event.getOperatorId(),
                event.getOperatorName(),
                event.getFromStatus(),
                event.getToStatus(),
                event.getRemark(),
                event.getCreatedAt()
        );
    }

    private String safeText(String value, String defaultValue) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return defaultValue;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}