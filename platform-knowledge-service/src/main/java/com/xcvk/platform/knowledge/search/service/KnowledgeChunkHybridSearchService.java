package com.xcvk.platform.knowledge.search.service;

import com.xcvk.platform.knowledge.model.dto.KnowledgeChunkHybridSearchRequest;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkHybridSearchItemVO;

import java.util.List;

/**
 * 知识文档切片混合检索服务
 *
 * <p>用于融合 chunk 全文检索和向量检索结果，
 * 为后续 AI / RAG 问答提供更稳定的知识片段召回能力。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
public interface KnowledgeChunkHybridSearchService {

    /**
     * 混合检索知识文档切片。
     *
     * @param request 混合检索请求
     * @return 融合后的知识片段列表
     */
    List<KnowledgeChunkHybridSearchItemVO> hybridSearch(KnowledgeChunkHybridSearchRequest request);
}