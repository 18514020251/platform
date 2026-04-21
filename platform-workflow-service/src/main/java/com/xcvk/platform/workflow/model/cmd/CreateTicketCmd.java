package com.xcvk.platform.workflow.model.cmd;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建工单命令对象
 *
 * <p>作为 workflow 模块内部统一的创建工单入口，
 * 手工创建和 AI 自动创建最终都应转换成该命令对象。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public record CreateTicketCmd(
        Long creatorId,
        String creatorName,
        String ticketTypeCode,
        String title,
        String content,
        String priority,
        String source,
        String sourceRef
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}