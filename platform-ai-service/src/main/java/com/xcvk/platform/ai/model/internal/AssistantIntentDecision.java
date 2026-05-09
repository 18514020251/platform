package com.xcvk.platform.ai.model.internal;

import java.io.Serial;
import java.io.Serializable;

/**
 * Assistant 意图识别结果。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-09
 */
public record AssistantIntentDecision(

        String intent,

        Double confidence,

        String ticketTypeCode,

        String title,

        String content,

        String priority

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}