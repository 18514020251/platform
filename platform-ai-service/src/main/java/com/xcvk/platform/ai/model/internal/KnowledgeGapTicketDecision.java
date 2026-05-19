package com.xcvk.platform.ai.model.internal;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识缺口转工单决策结果。
 *
 * <p>
 * 当 RAG 因无上下文或低相关性拒答后，
 * 由 LLM 判断用户问题是否属于企业内部知识缺口，
 * 并生成规范化后的工单信息。
 * </p>
 */
public record KnowledgeGapTicketDecision(

        /**
         * 是否属于企业内部问题。
         */
        Boolean enterpriseRelated,

        /**
         * LLM 判断置信度，范围 0.0 到 1.0。
         */
        Double confidence,

        /**
         * 规范化后的问题。
         */
        String normalizedQuestion,

        /**
         * 工单类型编码。
         */
        String ticketTypeCode,

        /**
         * 工单标题。
         */
        String title,

        /**
         * 工单内容。
         */
        String content,

        /**
         * 优先级：LOW / MEDIUM / HIGH。
         */
        String priority,

        /**
         * LLM 判断原因。
         */
        String reason

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}