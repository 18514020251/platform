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
 * <p>当前阶段采用 RRF（Reciprocal Rank Fusion）融合策略：</p>
 * <ul>
 *     <li>先执行 chunk 全文检索，获取关键词相关片段</li>
 *     <li>再执行 chunk 向量检索，获取语义相关片段</li>
 *     <li>按 chunkId 合并结果</li>
 *     <li>根据全文排名和向量排名计算 RRF 分数</li>
 *     <li>同时被全文和向量命中的片段给予轻量加分</li>
 * </ul>
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
     * RRF 排名平滑常数。
     *
     * <p>k 越大，靠前排名之间的分差越小；60 是搜索融合中常用的稳定默认值。</p>
     */
    private static final int RRF_K = 60;

    /**
     * 双路命中轻量奖励分。
     *
     * <p>用于鼓励同时被关键词和向量召回的片段，避免奖励过大导致压过真实排名。</p>
     */
    private static final float BOTH_MATCH_BONUS = 0.02F;

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

    private List<KnowledgeChunkVectorSearchItemVO> searchByVector(KnowledgeChunkHybridSearchRequest request,
                                                                  int recallSize) {
        KnowledgeChunkVectorSearchRequest vectorSearchRequest = new KnowledgeChunkVectorSearchRequest(
                request.question(),
                recallSize,
                request.categoryId()
        );

        return knowledgeChunkVectorSearchService.vectorSearch(vectorSearchRequest);
    }

    private Map<Long, HybridCandidate> mergeResults(List<KnowledgeChunkSearchItemVO> textResults,
                                                    List<KnowledgeChunkVectorSearchItemVO> vectorResults) {
        Map<Long, HybridCandidate> candidateMap = new LinkedHashMap<>();

        for (int i = 0; i < textResults.size(); i++) {
            KnowledgeChunkSearchItemVO item = textResults.get(i);
            if (item.chunkId() == null) {
                continue;
            }

            float textScore = rrfScore(i);
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

            float vectorScore = rrfScore(i);
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

    private float rrfScore(int index) {
        return 1.0F / (RRF_K + index + 1);
    }

    private int resolveRecallSize(int topK) {
        return Math.min(topK * RECALL_MULTIPLIER, MAX_RECALL_SIZE);
    }

    private void validateRequest(KnowledgeChunkHybridSearchRequest request) {
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.HYBRID_SEARCH_REQUEST_REQUIRED);
        BizAssert.isTrue(
                StringUtils.hasText(request.question()),
                ErrorCode.PARAM_INVALID,
                KnowledgeErrorMessages.HYBRID_SEARCH_QUESTION_REQUIRED
        );
    }

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
            float textPart = textRankScore == null ? 0.0F : textRankScore;
            float vectorPart = vectorRankScore == null ? 0.0F : vectorRankScore;
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