package com.xcvk.platform.workflow.model.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建工单响应
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public record CreateTicketResponse(
        Long ticketId,
        String ticketNo,
        String status
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}