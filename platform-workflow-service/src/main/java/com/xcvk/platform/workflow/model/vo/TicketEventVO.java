package com.xcvk.platform.workflow.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单操作流水展示对象
 */
public record TicketEventVO(
        Long eventId,
        Long ticketId,
        String ticketNo,
        String eventType,
        Long operatorId,
        String operatorName,
        String fromStatus,
        String toStatus,
        String remark,
        LocalDateTime createdAt
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}