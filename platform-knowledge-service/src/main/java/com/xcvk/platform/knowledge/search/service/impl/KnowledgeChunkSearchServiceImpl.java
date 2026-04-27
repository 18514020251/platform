package com.xcvk.platform.knowledge.search.service.impl;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.knowledge.constant.KnowledgeChunkStatusConstants;
import com.xcvk.platform.knowledge.constant.KnowledgeErrorMessages;
import com.xcvk.platform.knowledge.model.query.KnowledgeChunkSearchQuery;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkSearchItemVO;
import com.xcvk.platform.knowledge.search.constant.KnowledgeChunkSearchFields;
import com.xcvk.platform.knowledge.search.model.index.KnowledgeChunkIndex;
import com.xcvk.platform.knowledge.search.service.KnowledgeChunkSearchService;
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
 * 知识文档切片搜索服务实现类
 *
 * <p>当前阶段基于 Elasticsearch 提供 chunk 级全文检索能力，
 * 主要服务于后续 AI / RAG 场景中的知识片段召回。</p>
 *
 * <p>当前版本先不引入 embedding、dense_vector、kNN 等向量检索能力，
 * 先保证 chunk 级关键词检索链路清晰、可验证。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-27
 */
@Service
@RequiredArgsConstructor
public class KnowledgeChunkSearchServiceImpl implements KnowledgeChunkSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 搜索知识文档切片。
     *
     * @param query 搜索查询条件
     * @return 知识文档切片分页结果
     */
    @Override
    public PageResult<KnowledgeChunkSearchItemVO> searchChunks(KnowledgeChunkSearchQuery query) {
        BizAssert.notNull(query, ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.QUERY_REQUIRED);

        int pageNum = query.safePageNum();
        int pageSize = query.safePageSize();

        BoolQuery.Builder boolQuery = bool();
        applyChunkSearchFilters(boolQuery, query);

        NativeQuery searchQuery = buildSearchQuery(boolQuery, query, pageNum, pageSize);
        SearchHits<KnowledgeChunkIndex> searchHits =
                elasticsearchOperations.search(searchQuery, KnowledgeChunkIndex.class);

        return buildSearchPageResult(searchHits, pageNum, pageSize);
    }

    /**
     * 构建 chunk 搜索查询对象。
     *
     * <p>有关键词时优先使用 ES 默认相关性排序，适合后续 AI/RAG 召回；
     * 无关键词时按更新时间和 chunkNo 排序，便于开发调试查看。</p>
     *
     * @param boolQuery bool 查询条件
     * @param query 查询条件
     * @param pageNum 页码，从 1 开始
     * @param pageSize 分页大小
     * @return Elasticsearch 原生查询对象
     */
    private NativeQuery buildSearchQuery(BoolQuery.Builder boolQuery,
                                         KnowledgeChunkSearchQuery query,
                                         int pageNum,
                                         int pageSize) {
        var builder = NativeQuery.builder()
                .withQuery(boolQuery.build()._toQuery())
                .withPageable(PageRequest.of(pageNum - 1, pageSize))
                .withHighlightQuery(buildChunkTextHighlightQuery());

        if (!StringUtils.hasText(query.keyword())) {
            builder.withSort(buildDefaultSort());
        }

        return builder.build();
    }

    /**
     * 构建默认排序规则。
     *
     * <p>无关键词搜索时，优先按更新时间倒序，
     * 同一文档内按 chunkNo 正序展示。</p>
     *
     * @return 排序规则
     */
    private Sort buildDefaultSort() {
        return Sort.by(
                Sort.Order.desc(KnowledgeChunkSearchFields.UPDATED_AT),
                Sort.Order.asc(KnowledgeChunkSearchFields.CHUNK_NO)
        );
    }

    /**
     * 应用 chunk 搜索条件。
     *
     * @param boolQuery bool 查询构造器
     * @param query 查询条件
     */
    private void applyChunkSearchFilters(BoolQuery.Builder boolQuery,
                                         KnowledgeChunkSearchQuery query) {
        if (StringUtils.hasText(query.keyword())) {
            String keyword = query.keyword().trim();

            boolQuery.must(q -> q.bool(b -> b
                    .should(match(m -> m.field(KnowledgeChunkSearchFields.CHUNK_TEXT).query(keyword)))
                    .should(match(m -> m.field(KnowledgeChunkSearchFields.DOCUMENT_TITLE).query(keyword)))
                    .should(match(m -> m.field(KnowledgeChunkSearchFields.CATEGORY_NAME).query(keyword)))
                    .minimumShouldMatch(KnowledgeChunkSearchFields.MINIMUM_SHOULD_MATCH_ONE)
            ));
        } else {
            boolQuery.must(q -> q.matchAll(m -> m));
        }

        applyStatusFilter(boolQuery, query);

        if (query.documentId() != null) {
            boolQuery.filter(term(t -> t
                    .field(KnowledgeChunkSearchFields.DOCUMENT_ID)
                    .value(query.documentId())
            ));
        }

        if (query.categoryId() != null) {
            boolQuery.filter(term(t -> t
                    .field(KnowledgeChunkSearchFields.CATEGORY_ID)
                    .value(query.categoryId())
            ));
        }
    }

    /**
     * 应用 chunk 状态过滤。
     *
     * <p>默认只查询 ACTIVE 切片，避免 AI/RAG 召回已下线知识内容。</p>
     *
     * @param boolQuery bool 查询构造器
     * @param query 查询条件
     */
    private void applyStatusFilter(BoolQuery.Builder boolQuery,
                                   KnowledgeChunkSearchQuery query) {
        String status = StringUtils.hasText(query.status())
                ? query.status().trim()
                : KnowledgeChunkStatusConstants.ACTIVE;

        boolQuery.filter(term(t -> t
                .field(KnowledgeChunkSearchFields.STATUS)
                .value(status)
        ));
    }

    /**
     * 构建 chunkText 高亮查询配置。
     *
     * <p>当前阶段仅对 chunkText 字段做高亮展示，
     * 命中部分统一使用 em 标签包裹。</p>
     *
     * @return 高亮查询配置
     */
    private HighlightQuery buildChunkTextHighlightQuery() {
        return new HighlightQuery(
                new Highlight(
                        HighlightParameters.builder()
                                .withPreTags(KnowledgeChunkSearchFields.HIGHLIGHT_PRE_TAG)
                                .withPostTags(KnowledgeChunkSearchFields.HIGHLIGHT_POST_TAG)
                                .build(),
                        List.of(new HighlightField(KnowledgeChunkSearchFields.HIGHLIGHT_CHUNK_TEXT))
                ),
                KnowledgeChunkIndex.class
        );
    }

    /**
     * 将 Elasticsearch 搜索结果转换为分页返回对象。
     *
     * @param searchHits Elasticsearch 搜索结果
     * @param pageNum 当前页码
     * @param pageSize 当前分页大小
     * @return 知识文档切片分页结果
     */
    private PageResult<KnowledgeChunkSearchItemVO> buildSearchPageResult(SearchHits<KnowledgeChunkIndex> searchHits,
                                                                         int pageNum,
                                                                         int pageSize) {
        List<KnowledgeChunkSearchItemVO> records = searchHits.getSearchHits().stream()
                .map(this::toSearchItemVO)
                .toList();

        return PageResult.of(records, searchHits.getTotalHits(), pageNum, pageSize);
    }

    /**
     * 将 ES 搜索命中结果转换为 chunk 搜索列表项。
     *
     * @param searchHit ES 搜索命中结果
     * @return chunk 搜索列表项
     */
    private KnowledgeChunkSearchItemVO toSearchItemVO(SearchHit<KnowledgeChunkIndex> searchHit) {
        KnowledgeChunkIndex index = searchHit.getContent();

        String highlightChunkText = extractHighlightChunkText(searchHit);
        String chunkText = StringUtils.hasText(highlightChunkText)
                ? highlightChunkText
                : index.getChunkText();

        return new KnowledgeChunkSearchItemVO(
                index.getId(),
                index.getDocumentId(),
                index.getChunkNo(),
                chunkText,
                index.getDocumentTitle(),
                index.getCategoryId(),
                index.getCategoryName(),
                index.getTags(),
                index.getStatus(),
                index.getTokenCount(),
                index.getUpdatedAt()
        );
    }

    /**
     * 提取 chunkText 高亮内容。
     *
     * <p>如果当前搜索结果命中了 chunkText 字段并返回高亮片段，
     * 则优先使用高亮后的 chunkText；否则返回 null。</p>
     *
     * @param searchHit ES 搜索结果项
     * @return 高亮 chunkText；若无高亮则返回 null
     */
    private String extractHighlightChunkText(SearchHit<KnowledgeChunkIndex> searchHit) {
        if (searchHit == null) {
            return null;
        }

        Map<String, List<String>> highlightFields = searchHit.getHighlightFields();
        if (highlightFields.isEmpty()) {
            return null;
        }

        List<String> chunkTextHighlights = highlightFields.getOrDefault(
                KnowledgeChunkSearchFields.HIGHLIGHT_CHUNK_TEXT,
                Collections.emptyList()
        );
        if (chunkTextHighlights.isEmpty()) {
            return null;
        }

        return chunkTextHighlights.get(0);
    }
}