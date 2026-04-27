package com.xcvk.platform.knowledge.search.service;

import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.knowledge.model.query.KnowledgeChunkSearchQuery;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkSearchItemVO;

/**
 * 知识文档切片搜索服务
 *
 * <p>用于提供 chunk 级检索能力，当前阶段先实现全文检索；
 * 后续会在此基础上扩展向量检索和混合检索。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-27
 */
public interface KnowledgeChunkSearchService {

    /**
     * 搜索知识文档切片。
     *
     * @param query 搜索查询条件
     * @return 知识文档切片分页结果
     */
    PageResult<KnowledgeChunkSearchItemVO> searchChunks(KnowledgeChunkSearchQuery query);
}