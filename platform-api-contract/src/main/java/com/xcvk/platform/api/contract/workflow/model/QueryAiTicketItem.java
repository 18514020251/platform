package com.xcvk.platform.api.contract.workflow.model;

import java.time.LocalDateTime;

/**
 * AI 工单查询结果项。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-12
 */
public record QueryAiTicketItem(
        Long ticketId,

        String ticketNo,

        Long ticketTypeId,

        String ticketTypeCode,

        String ticketTypeName,

        String title,

        String content,

        String status,

        String priority,

        String source,

        String sourceRef,

        Long creatorId,

        String creatorName,

        Long assigneeId,

        String assigneeName,

        LocalDateTime closedAt,

        String statusRemark,

        LocalDateTime createdAt,

        LocalDateTime updatedAt
) {
}