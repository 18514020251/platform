package com.xcvk.platform.api.contract.knowledge.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 知识上下文召回请求
 *
 * <p>用于 AI 模块调用知识库模块，召回适合大模型 Prompt 使用的知识片段。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
public record KnowledgeRagContextRequest(

        /**
         * 用户问题
         */
        String question,

        /**
         * 召回数量
         */
        Integer topK,

        /**
         * 分类ID
         */
        Long categoryId

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}