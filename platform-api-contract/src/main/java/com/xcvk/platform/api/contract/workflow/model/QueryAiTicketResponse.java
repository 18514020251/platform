package com.xcvk.platform.api.contract.workflow.model;

import java.util.List;

/**
 * AI 工单查询响应。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-12
 */
public record QueryAiTicketResponse(
        String queryType,
        Integer total,
        List<QueryAiTicketItem> tickets
) {
}