package com.xcvk.platform.ai.model.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * 智能助手响应。
 *
 * <p>统一返回知识问答结果或 Agent Tool 执行结果。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-08
 */
public record AssistantChatResponse(

        /**
         * 本次识别出的意图。
         */
        String intent,

        /**
         * 返回给用户的自然语言结果。
         */
        String answer,

        /**
         * 是否执行了 Tool。
         */
        Boolean toolExecuted,

        /**
         * Tool 名称。
         */
        String toolName,

        /**
         * 工单创建结果。
         */
        AssistantTicketVO ticket,

        /**
         * RAG 问答结果。
         *
         * <p>知识问答场景有值，工单创建场景通常为空。</p>
         */
        RagChatResponse rag

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static AssistantChatResponse rag(String answer, RagChatResponse rag) {
        return new AssistantChatResponse(
                "KNOWLEDGE_QA",
                answer,
                false,
                null,
                null,
                rag
        );
    }

    public static AssistantChatResponse ticketCreated(String answer, AssistantTicketVO ticket) {
        return new AssistantChatResponse(
                "TICKET_CREATE",
                answer,
                true,
                "createTicket",
                ticket,
                null
        );
    }

    public static AssistantChatResponse ticketPendingConfirm(String answer) {
        return new AssistantChatResponse(
                "TICKET_CREATE",
                answer,
                false,
                "createTicket",
                null,
                null
        );
    }

    public static AssistantChatResponse unsupported(String answer) {
        return new AssistantChatResponse(
                "UNSUPPORTED_REQUEST",
                answer,
                false,
                null,
                null,
                null
        );
    }
}