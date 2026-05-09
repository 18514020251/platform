package com.xcvk.platform.ai.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;

/**
 * 智能助手请求。
 *
 * <p>统一承接知识问答、流程咨询、工单创建等用户自然语言请求。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-08
 */
public record AssistantChatRequest(

        @NotBlank(message = "问题不能为空")
        @Size(max = 500, message = "问题长度不能超过500个字符")
        String question,

        @Min(value = 1, message = "topK不能小于1")
        @Max(value = 10, message = "topK不能大于10")
        Integer topK,

        Long categoryId,

        /**
         * 创建人ID。
         *
         * <p>当前第一版先由请求传入，后续可以改为从 Sa-Token 登录态中获取。</p>
         */
        Long creatorId,

        /**
         * 创建人姓名。
         *
         * <p>当前第一版先由请求传入，后续可以改为从 Sa-Token 登录态中获取。</p>
         */
        String creatorName,

        /**
         * 是否允许 Agent 自动创建工单。
         *
         * <p>默认为 true。前端后续可通过该字段实现“先确认，再创建”。</p>
         */
        Boolean autoCreateTicket

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_TOP_K = 5;

    private static final int MAX_TOP_K = 10;

    public int safeTopK() {
        if (topK == null || topK < 1) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    public boolean autoCreateTicketOrTrue() {
        return autoCreateTicket == null || autoCreateTicket;
    }
}