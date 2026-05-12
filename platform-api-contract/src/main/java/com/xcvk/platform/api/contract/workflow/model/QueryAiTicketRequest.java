package com.xcvk.platform.api.contract.workflow.model;

/**
 * AI 工单查询请求。
 */
public record QueryAiTicketRequest(
        Long creatorId,
        Long assigneeId,
        String scope,
        String ticketNo,
        String status,
        Integer pageNum,
        Integer pageSize
) {
}