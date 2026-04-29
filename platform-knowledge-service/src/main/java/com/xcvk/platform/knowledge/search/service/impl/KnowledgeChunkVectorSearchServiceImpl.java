package com.xcvk.platform.knowledge.search.service.impl;

import co.elastic.clients.elasticsearch._types.KnnQuery;
import co.elastic.clients.json.JsonData;
import com.xcvk.platform.api.contract.ai.model.EmbeddingRequest;
import com.xcvk.platform.api.contract.ai.model.EmbeddingResponse;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.knowledge.constant.KnowledgeChunkStatusConstants;
import com.xcvk.platform.knowledge.constant.KnowledgeErrorMessages;
import com.xcvk.platform.knowledge.model.dto.KnowledgeChunkVectorSearchRequest;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkVectorSearchItemVO;
import com.xcvk.platform.knowledge.search.constant.KnowledgeChunkSearchFields;
import com.xcvk.platform.knowledge.search.model.index.KnowledgeChunkIndex;
import com.xcvk.platform.knowledge.search.service.KnowledgeChunkVectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.xcvk.platform.api.contract.ai.client.EmbedClient;

import java.util.ArrayList;
import java.util.List;

import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.exists;
import static co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders.term;

/**
 * 知识文档切片向量检索服务实现类
 *
 * <p>当前阶段基于 Elasticsearch kNN 检索 kb_chunk.embedding，
 * 用于验证 chunk 级语义检索能力。</p>
 *
 * <p>检索流程：</p>
 * <ul>
 *     <li>调用 ai-service 将用户问题转换为 query embedding</li>
 *     <li>使用 query embedding 在 kb_chunk.embedding 上执行 kNN 检索</li>
 *     <li>默认只召回 ACTIVE 状态的 chunk</li>
 *     <li>返回相似知识片段及 ES score</li>
 * </ul>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-28
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeChunkVectorSearchServiceImpl implements KnowledgeChunkVectorSearchService {

    /**
     * 当前 embedding 模型维度。
     *
     * <p>已通过 ai-service 调用结果确认 text-embedding-v3 返回 1024 维向量。</p>
     */
    private static final int EXPECTED_EMBEDDING_DIMENSION = 1024;

    /**
     * numCandidates 至少候选数量。
     *
     * <p>kNN 会先召回候选，再返回 topK。候选数略大于 topK，召回效果通常更稳定。</p>
     */
    private static final int MIN_NUM_CANDIDATES = 50;

    private final EmbedClient embedClient;

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 向量检索知识文档切片。
     *
     * @param request 向量检索请求
     * @return 相似知识片段列表
     */
    @Override
    public List<KnowledgeChunkVectorSearchItemVO> vectorSearch(KnowledgeChunkVectorSearchRequest request) {
        validateRequest(request);

        int topK = request.safeTopK();
        List<Float> queryVector = buildQueryVector(request.question());

        NativeQuery nativeQuery = buildVectorSearchQuery(request, queryVector, topK);
        SearchHits<KnowledgeChunkIndex> searchHits =
                elasticsearchOperations.search(nativeQuery, KnowledgeChunkIndex.class);

        List<KnowledgeChunkVectorSearchItemVO> records = searchHits.getSearchHits().stream()
                .map(this::toVectorSearchItemVO)
                .toList();

        log.info("知识文档切片向量检索完成，questionLength={}, topK={}, resultCount={}",
                request.question().length(), topK, records.size());

        return records;
    }

    /**
     * 构建用户问题向量。
     *
     * @param question 用户问题
     * @return 用户问题 embedding
     */
    private List<Float> buildQueryVector(String question) {
        EmbeddingResponse response = embedClient.embedTexts(
                new EmbeddingRequest(List.of(question.trim()))
        );

        validateQuestionEmbeddingResponse(response);

        return response.vectors().get(0);
    }

    /**
     * 构建向量检索查询。
     *
     * @param request 向量检索请求
     * @param queryVector 用户问题向量
     * @param topK 返回数量
     * @return ES 查询对象
     */
    private NativeQuery buildVectorSearchQuery(KnowledgeChunkVectorSearchRequest request,
                                               List<Float> queryVector,
                                               int topK) {
        Query scriptScoreQuery = Query.of(q -> q.scriptScore(scriptScore -> scriptScore
                .query(buildVectorBaseQuery(request))
                .script(script -> script.inline(inline -> inline
                        .lang("painless")
                        .source("cosineSimilarity(params.query_vector, 'embedding') + 1.0")
                        .params("query_vector", JsonData.of(queryVector))
                ))
        ));

        return NativeQuery.builder()
                .withQuery(scriptScoreQuery)
                .withPageable(PageRequest.of(0, topK))
                .build();
    }

    private Query buildVectorBaseQuery(KnowledgeChunkVectorSearchRequest request) {
        return Query.of(q -> q.bool(bool -> {
            bool.filter(term(t -> t
                    .field(KnowledgeChunkSearchFields.STATUS)
                    .value(KnowledgeChunkStatusConstants.ACTIVE)
            ));

            bool.filter(exists(e -> e
                    .field(KnowledgeChunkSearchFields.EMBEDDING)
            ));

            if (request.categoryId() != null) {
                bool.filter(term(t -> t
                        .field(KnowledgeChunkSearchFields.CATEGORY_ID)
                        .value(request.categoryId())
                ));
            }

            return bool;
        }));
    }

    /**
     * 转换向量检索结果项。
     *
     * @param searchHit ES 搜索命中
     * @return 向量检索结果项
     */
    private KnowledgeChunkVectorSearchItemVO toVectorSearchItemVO(SearchHit<KnowledgeChunkIndex> searchHit) {
        KnowledgeChunkIndex index = searchHit.getContent();

        return new KnowledgeChunkVectorSearchItemVO(
                index.getId(),
                index.getDocumentId(),
                index.getChunkNo(),
                index.getChunkText(),
                index.getDocumentTitle(),
                index.getCategoryId(),
                index.getCategoryName(),
                index.getTags(),
                searchHit.getScore()
        );
    }

    /**
     * 校验向量检索请求。
     *
     * @param request 向量检索请求
     */
    private void validateRequest(KnowledgeChunkVectorSearchRequest request) {
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.VECTOR_SEARCH_REQUEST_REQUIRED);
        BizAssert.hasText(request.question(), ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.VECTOR_SEARCH_QUESTION_REQUIRED);
    }

    /**
     * 校验用户问题 embedding 响应。
     *
     * @param response embedding 响应
     */
    private void validateQuestionEmbeddingResponse(EmbeddingResponse response) {
        BizAssert.notNull(response, ErrorCode.BIZ_ERROR, KnowledgeErrorMessages.EMBEDDING_RESPONSE_INVALID);
        BizAssert.hasText(response.modelName(), ErrorCode.BIZ_ERROR, KnowledgeErrorMessages.EMBEDDING_MODEL_REQUIRED);

        BizAssert.notNull(response.dimension(), ErrorCode.BIZ_ERROR, KnowledgeErrorMessages.EMBEDDING_DIMENSION_REQUIRED);
        BizAssert.isTrue(
                EXPECTED_EMBEDDING_DIMENSION == response.dimension(),
                ErrorCode.BIZ_ERROR,
                KnowledgeErrorMessages.EMBEDDING_VECTOR_DIMENSION_NOT_MATCH
        );

        BizAssert.isTrue(
                !CollectionUtils.isEmpty(response.vectors()),
                ErrorCode.BIZ_ERROR,
                KnowledgeErrorMessages.EMBEDDING_VECTOR_REQUIRED
        );

        BizAssert.isTrue(
                response.vectors().size() == 1,
                ErrorCode.BIZ_ERROR,
                KnowledgeErrorMessages.EMBEDDING_VECTOR_COUNT_NOT_MATCH
        );

        List<Float> queryVector = response.vectors().get(0);
        BizAssert.isTrue(
                !CollectionUtils.isEmpty(queryVector),
                ErrorCode.BIZ_ERROR,
                KnowledgeErrorMessages.EMBEDDING_VECTOR_REQUIRED
        );

        BizAssert.isTrue(
                queryVector.size() == EXPECTED_EMBEDDING_DIMENSION,
                ErrorCode.BIZ_ERROR,
                KnowledgeErrorMessages.EMBEDDING_VECTOR_DIMENSION_NOT_MATCH
        );
    }
}