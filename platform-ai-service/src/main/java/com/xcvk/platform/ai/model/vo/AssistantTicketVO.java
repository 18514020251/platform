package com.xcvk.platform.ai.model.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * Agent 创建工单结果。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-08
 */
public record AssistantTicketVO(

        Long ticketId,

        String ticketNo,

        String status,

        String ticketTypeCode,

        String title

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}