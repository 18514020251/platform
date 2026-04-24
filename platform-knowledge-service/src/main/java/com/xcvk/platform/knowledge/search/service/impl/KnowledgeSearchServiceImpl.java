package com.xcvk.platform.knowledge.search.service.impl;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.knowledge.constant.KnowledgeErrorMessages;
import com.xcvk.platform.knowledge.model.query.KnowledgeDocumentSearchQuery;
import com.xcvk.platform.knowledge.model.vo.KnowledgeDocumentSearchItemVO;
import com.xcvk.platform.knowledge.search.constant.KnowledgeSearchFields;
import com.xcvk.platform.knowledge.search.model.index.KnowledgeDocumentIndex;
import com.xcvk.platform.knowledge.search.service.KnowledgeSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 知识文档搜索服务实现类
 *
 * <p>当前阶段先实现基于 Elasticsearch 的文档级全文检索，
 * 主要搜索 title、summary、content 等字段。</p>
 *
 * <p>后续向量检索会基于 chunk 级索引扩展，不直接污染当前文档级全文搜索主流程。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-24
 */
@Service
@RequiredArgsConstructor
public class KnowledgeSearchServiceImpl implements KnowledgeSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 搜索知识文档。
     *
     * @param query 搜索查询条件
     * @return 知识文档分页结果
     */
    @Override
    public PageResult<KnowledgeDocumentSearchItemVO> searchDocuments(KnowledgeDocumentSearchQuery query) {
        BizAssert.notNull(query, ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.QUERY_REQUIRED);

        int pageNum = query.safePageNum();
        int pageSize = query.safePageSize();

        NativeQuery nativeQuery = NativeQuery.builder()
                .withQuery(buildSearchQuery(query))
                .withPageable(PageRequest.of(pageNum - 1, pageSize))
                .withSort(sort -> sort.field(field -> field
                        .field(KnowledgeSearchFields.UPDATED_AT)
                        .order(SortOrder.Desc)
                ))
                .build();

        var searchHits = elasticsearchOperations.search(nativeQuery, KnowledgeDocumentIndex.class);

        List<KnowledgeDocumentSearchItemVO> records = searchHits.getSearchHits()
                .stream()
                .map(this::toSearchItemVO)
                .toList();

        return PageResult.of(records, searchHits.getTotalHits(), pageNum, pageSize);
    }

    /**
     * 构建知识文档搜索查询。
     *
     * @param query 搜索查询条件
     * @return ES 查询对象
     */
    private Query buildSearchQuery(KnowledgeDocumentSearchQuery query) {
        return Query.of(q -> q.bool(bool -> {
            if (StringUtils.hasText(query.keyword())) {
                String keyword = query.keyword().trim();

                bool.must(must -> must.multiMatch(multiMatch -> multiMatch
                        .query(keyword)
                        .fields(
                                KnowledgeSearchFields.TITLE,
                                KnowledgeSearchFields.SUMMARY,
                                KnowledgeSearchFields.CONTENT,
                                KnowledgeSearchFields.CATEGORY_NAME
                        )
                ));
            } else {
                bool.must(must -> must.matchAll(matchAll -> matchAll));
            }

            if (StringUtils.hasText(query.status())) {
                bool.filter(filter -> filter.term(term -> term
                        .field(KnowledgeSearchFields.STATUS)
                        .value(query.status().trim())
                ));
            }

            if (query.categoryId() != null) {
                bool.filter(filter -> filter.term(term -> term
                        .field(KnowledgeSearchFields.CATEGORY_ID)
                        .value(query.categoryId())
                ));
            }

            return bool;
        }));
    }

    /**
     * 将 ES 搜索命中结果转换为列表展示对象。
     *
     * @param searchHit ES 搜索命中结果
     * @return 知识文档搜索列表项
     */
    private KnowledgeDocumentSearchItemVO toSearchItemVO(SearchHit<KnowledgeDocumentIndex> searchHit) {
        KnowledgeDocumentIndex index = searchHit.getContent();

        return new KnowledgeDocumentSearchItemVO(
                index.getDocumentId(),
                index.getTitle(),
                index.getSummary(),
                index.getCategoryId(),
                index.getCategoryName(),
                index.getTags(),
                index.getStatus(),
                index.getCreatorName(),
                index.getPublishedAt(),
                index.getUpdatedAt()
        );
    }
}