package com.xcvk.platform.workflow.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单列表项
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public record TicketListItemVO(
        Long ticketId,
        String ticketNo,
        String ticketTypeCode,
        String ticketTypeName,
        String title,
        String status,
        String priority,
        String source,
        String assigneeName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}