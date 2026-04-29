package com.xcvk.platform.knowledge.search.service;

import com.xcvk.platform.knowledge.model.dto.KnowledgeChunkVectorSearchRequest;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkVectorSearchItemVO;

import java.util.List;

/**
 * 知识文档切片向量检索服务
 *
 * <p>用于基于用户问题 embedding，在 kb_chunk 向量索引中检索语义相似的知识片段。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-28
 */
public interface KnowledgeChunkVectorSearchService {

    /**
     * 向量检索知识文档切片。
     *
     * @param request 向量检索请求
     * @return 相似知识片段列表
     */
    List<KnowledgeChunkVectorSearchItemVO> vectorSearch(KnowledgeChunkVectorSearchRequest request);
}