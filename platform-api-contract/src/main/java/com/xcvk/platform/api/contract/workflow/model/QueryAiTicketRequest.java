package com.xcvk.platform.api.contract.workflow.model;

/**
 *  工单查询请求。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-12 19:56
 */
public record QueryAiTicketRequest(
        Long creatorId,
        String ticketNo,
        String status,
        Integer pageNum,
        Integer pageSize
) {
}
