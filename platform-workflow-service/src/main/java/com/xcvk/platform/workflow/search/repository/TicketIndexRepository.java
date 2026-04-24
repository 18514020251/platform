package com.xcvk.platform.workflow.search.repository;

import com.xcvk.platform.workflow.search.model.index.TicketIndex;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 工单搜索仓储接口
 *
 * <p>该接口用于承接 TicketIndex 在 Elasticsearch 中的基础读写操作，
 * 当前阶段先复用 Spring Data Elasticsearch 提供的通用 CRUD 能力。</p>
 *
 * <p>后续如需扩展复杂搜索条件、分页查询、高亮、聚合统计等能力，
 * 可以在此基础上增加自定义搜索仓储实现。</p>
 *
 * @author Programmer
 * @since 2026-04-23
 */
@Repository
public interface TicketIndexRepository extends
        ElasticsearchRepository<TicketIndex, Long> {
}