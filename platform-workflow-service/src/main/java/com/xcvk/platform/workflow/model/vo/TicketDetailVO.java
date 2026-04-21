package com.xcvk.platform.workflow.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单详情
 *
 * <p>当前阶段只返回工单主表中的当前态信息，
 * 历史记录后续由 MongoDB 承接后再补充。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public record TicketDetailVO(
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
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}