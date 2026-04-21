package com.xcvk.platform.workflow.model.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单类型选项
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public record TicketTypeOptionVO(
        Long ticketTypeId,
        String typeCode,
        String typeName,
        String defaultPriority
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}