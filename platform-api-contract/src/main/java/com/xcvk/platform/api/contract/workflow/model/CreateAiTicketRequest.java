package com.xcvk.platform.api.contract.workflow.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;

/**
 * AI 创建工单请求。
 *
 * <p>该对象用于 ai-service 调用 workflow-service 内部接口，
 * 将 Agent 识别出的工单意图转化为真实工单。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-08
 */
public record CreateAiTicketRequest(

        @NotNull(message = "创建人ID不能为空")
        Long creatorId,

        @NotBlank(message = "创建人名称不能为空")
        String creatorName,

        @NotBlank(message = "工单类型编码不能为空")
        String ticketTypeCode,

        @NotBlank(message = "工单标题不能为空")
        @Size(max = 128, message = "工单标题长度不能超过128个字符")
        String title,

        @NotBlank(message = "工单内容不能为空")
        @Size(max = 2000, message = "工单内容长度不能超过2000个字符")
        String content,

        String priority,

        /**
         * AI 会话ID / Agent 执行ID。
         *
         * <p>用于后续追踪“哪一次 Agent 调用创建了该工单”。</p>
         */
        String sourceRef

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}