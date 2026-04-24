package com.xcvk.platform.knowledge.search.repository;

import com.xcvk.platform.knowledge.search.model.index.KnowledgeDocumentIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 知识文档搜索仓储接口
 *
 * <p>该接口用于承接 KnowledgeDocumentIndex 在 Elasticsearch 中的基础读写操作，
 * 当前阶段先复用 Spring Data Elasticsearch 提供的通用 CRUD 能力。</p>
 *
 * <p>后续如需扩展复杂搜索条件、分页查询、高亮、聚合统计、向量检索等能力，
 * 可以在此基础上增加自定义搜索仓储实现。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-24 9:57
 */
@Repository
public interface KnowledgeDocumentIndexRepository
        extends ElasticsearchRepository<KnowledgeDocumentIndex, Long> {
}
