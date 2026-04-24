package com.xcvk.platform.knowledge.search.service;

import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.knowledge.model.query.KnowledgeDocumentSearchQuery;
import com.xcvk.platform.knowledge.model.vo.KnowledgeDocumentSearchItemVO;

/**
 * 知识文档搜索服务
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-24
 */
public interface KnowledgeSearchService {

    /**
     * 搜索知识文档。
     *
     * @param query 搜索查询条件
     * @return 知识文档分页结果
     */
    PageResult<KnowledgeDocumentSearchItemVO> searchDocuments(KnowledgeDocumentSearchQuery query);
}