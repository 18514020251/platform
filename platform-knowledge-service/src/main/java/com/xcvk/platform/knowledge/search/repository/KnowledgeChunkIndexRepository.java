package com.xcvk.platform.knowledge.search.repository;

import com.xcvk.platform.knowledge.search.model.index.KnowledgeChunkIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 知识文档切片搜索仓储接口
 *
 * <p>该接口用于承接 KnowledgeChunkIndex 在 Elasticsearch 中的基础读写操作，
 * 当前阶段先复用 Spring Data Elasticsearch 提供的通用 CRUD 能力。</p>
 *
 * <p>后续如需扩展 chunk 级全文检索、向量检索、混合检索等能力，
 * 可以在此基础上增加自定义搜索仓储实现。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-26
 */
@Repository
public interface KnowledgeChunkIndexRepository extends ElasticsearchRepository<KnowledgeChunkIndex, Long> {
}