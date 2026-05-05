package com.xcvk.platform.ai.model.vo;

import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextItem;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * RAG 问答响应
 *
 * <p>包含大模型生成的回答，以及本次回答参考的知识库上下文。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
public record RagChatResponse(

        /**
         * 大模型回答内容
         */
        String answer,

        /**
         * 本次回答参考的知识上下文
         */
        List<KnowledgeRagContextItem> contexts

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}