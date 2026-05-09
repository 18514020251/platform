package com.xcvk.platform.ai.model.internal;

import java.io.Serial;
import java.io.Serializable;

/**
 * 工单业务范围校验结果。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-09
 */
public record TicketScopeValidation(

        boolean passed,

        String reason

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static TicketScopeValidation pass() {
        return new TicketScopeValidation(true, null);
    }

    public static TicketScopeValidation reject(String reason) {
        return new TicketScopeValidation(false, reason);
    }
}