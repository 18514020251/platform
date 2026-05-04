package com.xcvk.platform.knowledge.search.service.impl;

import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.knowledge.constant.KnowledgeChunkStatusConstants;
import com.xcvk.platform.knowledge.constant.KnowledgeErrorMessages;
import com.xcvk.platform.knowledge.model.dto.KnowledgeChunkHybridSearchRequest;
import com.xcvk.platform.knowledge.model.dto.KnowledgeChunkVectorSearchRequest;
import com.xcvk.platform.knowledge.model.query.KnowledgeChunkSearchQuery;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkHybridSearchItemVO;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkSearchItemVO;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkVectorSearchItemVO;
import com.xcvk.platform.knowledge.search.service.KnowledgeChunkHybridSearchService;
import com.xcvk.platform.knowledge.search.service.KnowledgeChunkSearchService;
import com.xcvk.platform.knowledge.search.service.KnowledgeChunkVectorSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识文档切片混合检索服务实现类
 *
 * <p>当前阶段采用简单、可解释的融合策略：</p>
 * <ul>
 *     <li>先执行 chunk 全文检索，获取关键词相关片段</li>
 *     <li>再执行 chunk 向量检索，获取语义相关片段</li>
 *     <li>按 chunkId 合并结果</li>
 *     <li>同时被全文和向量命中的片段给予额外加分</li>
 * </ul>
 *
 * <p>后续如果需要更稳定的排序效果，可以升级为 RRF 等融合算法。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeChunkHybridSearchServiceImpl implements KnowledgeChunkHybridSearchService {

    /**
     * 全文检索权重
     */
    private static final float TEXT_WEIGHT = 0.4F;

    /**
     * 向量检索权重
     */
    private static final float VECTOR_WEIGHT = 0.6F;

    /**
     * 双路命中奖励分
     */
    private static final float BOTH_MATCH_BONUS = 0.5F;

    /**
     * 混合检索内部召回倍数
     *
     * <p>先多召回一些候选，再融合排序取最终 topK。</p>
     */
    private static final int RECALL_MULTIPLIER = 2;

    /**
     * 最大内部召回数量
     */
    private static final int MAX_RECALL_SIZE = 20;

    private static final String MATCH_TYPE_TEXT = "TEXT";

    private static final String MATCH_TYPE_VECTOR = "VECTOR";

    private static final String MATCH_TYPE_BOTH = "BOTH";

    private final KnowledgeChunkSearchService knowledgeChunkSearchService;

    private final KnowledgeChunkVectorSearchService knowledgeChunkVectorSearchService;

    /**
     * 混合检索知识文档切片。
     *
     * @param request 混合检索请求
     * @return 融合后的知识片段列表
     */
    @Override
    public List<KnowledgeChunkHybridSearchItemVO> hybridSearch(KnowledgeChunkHybridSearchRequest request) {
        validateRequest(request);

        int topK = request.safeTopK();
        int recallSize = resolveRecallSize(topK);

        List<KnowledgeChunkSearchItemVO> textResults = searchByText(request, recallSize);
        List<KnowledgeChunkVectorSearchItemVO> vectorResults = searchByVector(request, recallSize);

        List<KnowledgeChunkHybridSearchItemVO> records = mergeResults(textResults, vectorResults)
                .values()
                .stream()
                .map(HybridCandidate::toVO)
                .sorted(Comparator.comparing(KnowledgeChunkHybridSearchItemVO::finalScore).reversed())
                .limit(topK)
                .toList();

        log.info("知识文档切片混合检索完成，questionLength={}, topK={}, textCount={}, vectorCount={}, resultCount={}",
                request.question().length(),
                topK,
                textResults.size(),
                vectorResults.size(),
                records.size()
        );

        return records;
    }

    /**
     * 执行 chunk 全文检索。
     *
     * @param request 混合检索请求
     * @param recallSize 内部召回数量
     * @return 全文检索结果
     */
    private List<KnowledgeChunkSearchItemVO> searchByText(KnowledgeChunkHybridSearchRequest request, int recallSize) {
        KnowledgeChunkSearchQuery query = new KnowledgeChunkSearchQuery(
                request.question(),
                null,
                request.categoryId(),
                KnowledgeChunkStatusConstants.ACTIVE,
                1,
                recallSize
        );

        PageResult<KnowledgeChunkSearchItemVO> pageResult = knowledgeChunkSearchService.searchChunks(query);
        if (pageResult == null || pageResult.getRecords() == null) {
            return List.of();
        }

        return pageResult.getRecords();
    }

    /**
     * 执行 chunk 向量检索。
     *
     * @param request 混合检索请求
     * @param recallSize 内部召回数量
     * @return 向量检索结果
     */
    private List<KnowledgeChunkVectorSearchItemVO> searchByVector(KnowledgeChunkHybridSearchRequest request,
                                                                  int recallSize) {
        KnowledgeChunkVectorSearchRequest vectorSearchRequest = new KnowledgeChunkVectorSearchRequest(
                request.question(),
                recallSize,
                request.categoryId()
        );

        return knowledgeChunkVectorSearchService.vectorSearch(vectorSearchRequest);
    }

    /**
     * 合并全文检索和向量检索结果。
     *
     * <p>当前阶段使用 rank-based score：
     * 排名越靠前，基础分越高；双路都命中时额外加分。</p>
     *
     * @param textResults 全文检索结果
     * @param vectorResults 向量检索结果
     * @return chunkId 到候选结果的映射
     */
    private Map<Long, HybridCandidate> mergeResults(List<KnowledgeChunkSearchItemVO> textResults,
                                                    List<KnowledgeChunkVectorSearchItemVO> vectorResults) {
        Map<Long, HybridCandidate> candidateMap = new LinkedHashMap<>();

        for (int i = 0; i < textResults.size(); i++) {
            KnowledgeChunkSearchItemVO item = textResults.get(i);
            if (item.chunkId() == null) {
                continue;
            }

            float textScore = rankScore(i);
            HybridCandidate candidate = candidateMap.computeIfAbsent(
                    item.chunkId(),
                    ignored -> HybridCandidate.fromText(item)
            );

            candidate.textRankScore = textScore;
            candidate.matchedByText = true;
        }

        for (int i = 0; i < vectorResults.size(); i++) {
            KnowledgeChunkVectorSearchItemVO item = vectorResults.get(i);
            if (item.chunkId() == null) {
                continue;
            }

            float vectorScore = rankScore(i);
            HybridCandidate candidate = candidateMap.computeIfAbsent(
                    item.chunkId(),
                    ignored -> HybridCandidate.fromVector(item)
            );

            candidate.vectorRankScore = vectorScore;
            candidate.vectorRawScore = item.score();
            candidate.matchedByVector = true;
        }

        candidateMap.values().forEach(HybridCandidate::calculateFinalScore);
        return candidateMap;
    }

    /**
     * 根据排名计算基础得分。
     *
     * <p>排名从 0 开始，第一名得 1 分，第二名得 0.5 分，第三名得 0.333 分。</p>
     *
     * @param index 排名下标
     * @return 排名得分
     */
    private float rankScore(int index) {
        return 1.0F / (index + 1);
    }

    /**
     * 计算内部召回数量。
     *
     * @param topK 最终返回数量
     * @return 内部召回数量
     */
    private int resolveRecallSize(int topK) {
        return Math.min(topK * RECALL_MULTIPLIER, MAX_RECALL_SIZE);
    }

    /**
     * 校验混合检索请求。
     *
     * @param request 混合检索请求
     */
    private void validateRequest(KnowledgeChunkHybridSearchRequest request) {
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.HYBRID_SEARCH_REQUEST_REQUIRED);
        BizAssert.isTrue(
                StringUtils.hasText(request.question()),
                ErrorCode.PARAM_INVALID,
                KnowledgeErrorMessages.HYBRID_SEARCH_QUESTION_REQUIRED
        );
    }

    /**
     * 混合检索候选项。
     *
     * <p>用于在 Service 内部合并全文检索和向量检索结果，不对外暴露。</p>
     */
    private static class HybridCandidate {

        private Long chunkId;

        private Long documentId;

        private Integer chunkNo;

        private String chunkText;

        private String documentTitle;

        private Long categoryId;

        private String categoryName;

        private String tags;

        private Float textRankScore;

        private Float vectorRankScore;

        private Float vectorRawScore;

        private Float finalScore;

        private boolean matchedByText;

        private boolean matchedByVector;

        static HybridCandidate fromText(KnowledgeChunkSearchItemVO item) {
            HybridCandidate candidate = new HybridCandidate();
            candidate.chunkId = item.chunkId();
            candidate.documentId = item.documentId();
            candidate.chunkNo = item.chunkNo();
            candidate.chunkText = item.chunkText();
            candidate.documentTitle = item.documentTitle();
            candidate.categoryId = item.categoryId();
            candidate.categoryName = item.categoryName();
            candidate.tags = item.tags();
            return candidate;
        }

        static HybridCandidate fromVector(KnowledgeChunkVectorSearchItemVO item) {
            HybridCandidate candidate = new HybridCandidate();
            candidate.chunkId = item.chunkId();
            candidate.documentId = item.documentId();
            candidate.chunkNo = item.chunkNo();
            candidate.chunkText = item.chunkText();
            candidate.documentTitle = item.documentTitle();
            candidate.categoryId = item.categoryId();
            candidate.categoryName = item.categoryName();
            candidate.tags = item.tags();
            candidate.vectorRawScore = item.score();
            return candidate;
        }

        void calculateFinalScore() {
            float textPart = textRankScore == null ? 0.0F : textRankScore * TEXT_WEIGHT;
            float vectorPart = vectorRankScore == null ? 0.0F : vectorRankScore * VECTOR_WEIGHT;
            float bonus = matchedByText && matchedByVector ? BOTH_MATCH_BONUS : 0.0F;
            this.finalScore = textPart + vectorPart + bonus;
        }

        KnowledgeChunkHybridSearchItemVO toVO() {
            return new KnowledgeChunkHybridSearchItemVO(
                    chunkId,
                    documentId,
                    chunkNo,
                    chunkText,
                    documentTitle,
                    categoryId,
                    categoryName,
                    tags,
                    textRankScore,
                    vectorRankScore,
                    vectorRawScore,
                    finalScore,
                    resolveMatchType()
            );
        }

        private String resolveMatchType() {
            if (matchedByText && matchedByVector) {
                return MATCH_TYPE_BOTH;
            }
            if (matchedByText) {
                return MATCH_TYPE_TEXT;
            }
            return MATCH_TYPE_VECTOR;
        }
    }
}