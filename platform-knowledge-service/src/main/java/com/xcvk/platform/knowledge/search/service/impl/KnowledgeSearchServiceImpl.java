package com.xcvk.platform.knowledge.search.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.*;

/**
 * 知识文档搜索服务实现类
 *
 * <p>当前阶段基于 Elasticsearch 提供知识文档全文检索能力，
 * 实现最小可用的搜索闭环：</p>
 * <ul>
 *     <li>关键词搜索：title、summary、content、categoryName</li>
 *     <li>条件过滤：status、categoryId</li>
 *     <li>分页与排序</li>
 *     <li>标题高亮</li>
 * </ul>
 *
 * <p>当前版本先不引入向量检索、混合检索、聚合统计等增强能力，
 * 优先保证文档级全文检索链路清晰、易懂、可验证。</p>
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

        BoolQuery.Builder boolQuery = bool();
        applySearchQueryFilters(boolQuery, query);

        NativeQuery searchQuery = buildSearchQuery(boolQuery, pageNum, pageSize);
        SearchHits<KnowledgeDocumentIndex> searchHits =
                elasticsearchOperations.search(searchQuery, KnowledgeDocumentIndex.class);

        return buildSearchPageResult(searchHits, pageNum, pageSize);
    }

    /**
     * 构建知识文档搜索查询对象。
     *
     * <p>统一收口分页、排序和高亮配置，
     * 避免主流程方法中混入过多 Elasticsearch 查询细节。</p>
     *
     * @param boolQuery bool 查询条件
     * @param pageNum 页码，从 1 开始
     * @param pageSize 分页大小
     * @return Elasticsearch 原生查询对象
     */
    private NativeQuery buildSearchQuery(BoolQuery.Builder boolQuery, int pageNum, int pageSize) {
        return NativeQuery.builder()
                .withQuery(boolQuery.build()._toQuery())
                .withPageable(PageRequest.of(pageNum - 1, pageSize))
                .withSort(buildSort())
                .withHighlightQuery(buildTitleHighlightQuery())
                .build();
    }

    /**
     * 构建知识文档搜索排序规则。
     *
     * <p>当前阶段知识文档列表优先按更新时间倒序，
     * 更新时间相同则按创建时间倒序。</p>
     *
     * @return 排序规则
     */
    private Sort buildSort() {
        return Sort.by(
                Sort.Order.desc(KnowledgeSearchFields.UPDATED_AT),
                Sort.Order.desc(KnowledgeSearchFields.CREATED_AT)
        );
    }

    /**
     * 应用知识文档搜索条件。
     *
     * @param boolQuery bool 查询构造器
     * @param query 查询条件
     */
    private void applySearchQueryFilters(BoolQuery.Builder boolQuery,
                                         KnowledgeDocumentSearchQuery query) {
        if (StringUtils.hasText(query.keyword())) {
            String keyword = query.keyword().trim();

            boolQuery.must(q -> q.bool(b -> b
                    .should(match(m -> m.field(KnowledgeSearchFields.TITLE).query(keyword)))
                    .should(match(m -> m.field(KnowledgeSearchFields.SUMMARY).query(keyword)))
                    .should(match(m -> m.field(KnowledgeSearchFields.CONTENT).query(keyword)))
                    .should(match(m -> m.field(KnowledgeSearchFields.CATEGORY_NAME).query(keyword)))
                    .minimumShouldMatch(KnowledgeSearchFields.MINIMUM_SHOULD_MATCH_ONE)
            ));
        }else {
            boolQuery.must(q -> q.matchAll(m -> m));
        }

        if (StringUtils.hasText(query.status())) {
            boolQuery.filter(term(t -> t.field(KnowledgeSearchFields.STATUS).value(query.status().trim())));
        }

        if (query.categoryId() != null) {
            boolQuery.filter(term(t -> t.field(KnowledgeSearchFields.CATEGORY_ID).value(query.categoryId())));
        }
    }

    /**
     * 构建标题高亮查询配置。
     *
     * <p>当前阶段仅对 title 字段做高亮展示，
     * 命中部分统一使用 em 标签包裹。</p>
     *
     * @return 高亮查询配置
     */
    private HighlightQuery buildTitleHighlightQuery() {
        return new HighlightQuery(
                new Highlight(
                        HighlightParameters.builder()
                                .withPreTags(KnowledgeSearchFields.HIGHLIGHT_PRE_TAG)
                                .withPostTags(KnowledgeSearchFields.HIGHLIGHT_POST_TAG)
                                .build(),
                        List.of(new HighlightField(KnowledgeSearchFields.HIGHLIGHT_TITLE))
                ),
                KnowledgeDocumentIndex.class
        );
    }

    /**
     * 将 Elasticsearch 搜索结果转换为分页返回对象。
     *
     * <p>如果搜索结果中存在标题高亮，则优先使用高亮标题；
     * 否则回退原始标题。</p>
     *
     * @param searchHits Elasticsearch 搜索结果
     * @param pageNum 当前页码
     * @param pageSize 当前分页大小
     * @return 知识文档分页结果
     */
    private PageResult<KnowledgeDocumentSearchItemVO> buildSearchPageResult(
            SearchHits<KnowledgeDocumentIndex> searchHits,
            int pageNum,
            int pageSize) {

        List<KnowledgeDocumentSearchItemVO> records = searchHits.getSearchHits().stream()
                .map(this::toSearchItemVO)
                .toList();

        return PageResult.of(records, searchHits.getTotalHits(), pageNum, pageSize);
    }

    /**
     * 将 ES 搜索命中结果转换为列表展示对象。
     *
     * @param searchHit ES 搜索命中结果
     * @return 知识文档搜索列表项
     */
    private KnowledgeDocumentSearchItemVO toSearchItemVO(SearchHit<KnowledgeDocumentIndex> searchHit) {
        KnowledgeDocumentIndex index = searchHit.getContent();
        String highlightTitle = extractHighlightTitle(searchHit);
        String title = StringUtils.hasText(highlightTitle) ? highlightTitle : index.getTitle();

        return new KnowledgeDocumentSearchItemVO(
                index.getDocumentId(),
                title,
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

    /**
     * 提取标题高亮内容。
     *
     * <p>如果当前搜索结果命中了 title 字段并返回高亮片段，
     * 则优先使用高亮后的标题；否则返回 null，由转换方法回退到原始标题。</p>
     *
     * @param searchHit ES 搜索结果项
     * @return 高亮标题；若无高亮则返回 null
     */
    private String extractHighlightTitle(SearchHit<KnowledgeDocumentIndex> searchHit) {
        if (searchHit == null) {
            return null;
        }

        Map<String, List<String>> highlightFields = searchHit.getHighlightFields();
        if (highlightFields.isEmpty()) {
            return null;
        }

        List<String> titleHighlights = highlightFields.getOrDefault(
                KnowledgeSearchFields.HIGHLIGHT_TITLE,
                Collections.emptyList()
        );
        if (titleHighlights.isEmpty()) {
            return null;
        }

        return titleHighlights.get(0);
    }
}