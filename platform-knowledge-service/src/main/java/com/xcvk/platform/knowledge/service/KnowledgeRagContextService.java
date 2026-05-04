package com.xcvk.platform.knowledge.service;

import com.xcvk.platform.knowledge.model.dto.KnowledgeChunkHybridSearchRequest;
import com.xcvk.platform.knowledge.model.vo.KnowledgeRagContextItemVO;

import java.util.List;

/**
 * RAG 知识上下文召回服务
 *
 * <p>用于基于混合检索召回适合大模型使用的知识片段。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
public interface KnowledgeRagContextService {

    /**
     * 召回 RAG 知识上下文。
     *
     * @param request 混合检索请求
     * @return 干净的知识上下文片段列表
     */
    List<KnowledgeRagContextItemVO> retrieveContexts(KnowledgeChunkHybridSearchRequest request);
}