package com.xcvk.platform.ai.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * RAG 问答响应。
 *
 * <p>包含大模型生成的回答、结构化引用来源以及本次回答是否因证据不足被拒答。</p>
 *
 * @author Programmer
 * @version 1.1
 * @date 2026-05-08
 */
public record RagChatResponse(

        /**
         * 大模型回答内容
         */
        String answer,

        /**
         * 本次回答引用来源
         */
        List<RagCitation> citations,

        /**
         * 是否因为知识库证据不足而拒答
         */
        Boolean rejected,

        /**
         * 拒答原因。非拒答时为 null
         */
        String rejectReason

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static RagChatResponse answered(String answer, List<RagCitation> citations) {
        return new RagChatResponse(answer, citations, false, null);
    }

    public static RagChatResponse rejected(String answer, String rejectReason, List<RagCitation> citations) {
        return new RagChatResponse(answer, citations, true, rejectReason);
    }
}