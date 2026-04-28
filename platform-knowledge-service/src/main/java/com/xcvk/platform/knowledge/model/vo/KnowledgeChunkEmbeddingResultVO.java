package com.xcvk.platform.knowledge.model.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识文档切片向量化结果
 *
 * <p>用于开发阶段验证 knowledge-service 调用 ai-service 生成 embedding 的结果。</p>
 *
 * <p>注意：该对象不直接返回完整向量，避免响应体过大。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-28
 */
public record KnowledgeChunkEmbeddingResultVO(

        /**
         * 知识文档ID
         */
        Long documentId,

        /**
         * 向量模型名称
         */
        String modelName,

        /**
         * 向量维度
         */
        Integer dimension,

        /**
         * 生成向量数量
         */
        Integer vectorCount

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}