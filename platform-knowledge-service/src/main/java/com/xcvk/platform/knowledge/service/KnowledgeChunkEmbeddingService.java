package com.xcvk.platform.knowledge.service;

import com.xcvk.platform.api.contract.ai.model.EmbeddingResponse;

/**
 * 知识文档切片向量化服务
 *
 * <p>用于调用 AI 模块，将知识文档 chunk 文本转换为 embedding 向量。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-28
 */
public interface KnowledgeChunkEmbeddingService {

    /**
     * 为指定知识文档的 ACTIVE 切片生成 embedding。
     *
     * <p>当前阶段只负责调用 AI 模块并校验返回结果，
     * 暂不写入 Elasticsearch dense_vector 字段。</p>
     *
     * @param documentId 知识文档ID
     * @return 文本向量化响应
     */
    EmbeddingResponse embedDocumentChunks(Long documentId);
}