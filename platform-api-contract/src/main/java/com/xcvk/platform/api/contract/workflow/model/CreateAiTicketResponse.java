package com.xcvk.platform.api.contract.workflow.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 创建工单响应。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-08
 */
public record CreateAiTicketResponse(

        Long ticketId,

        String ticketNo,

        String status

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}