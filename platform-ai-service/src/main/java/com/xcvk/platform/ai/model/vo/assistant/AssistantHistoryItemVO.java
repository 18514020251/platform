package com.xcvk.platform.ai.model.vo.assistant;

import java.time.LocalDateTime;

/**
 *  助手历史记录项
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-18 3:33
 */
public record AssistantHistoryItemVO(

        Long executionLogId,

        String question,

        String intent,

        String executionStatus,

        Boolean toolExecuted,

        String toolName,

        LocalDateTime createdAt

) {
}
