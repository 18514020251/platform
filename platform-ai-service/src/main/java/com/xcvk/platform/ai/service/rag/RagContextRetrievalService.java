package com.xcvk.platform.ai.service.rag;

import com.xcvk.platform.ai.model.vo.RagCitation;
import com.xcvk.platform.api.contract.knowledge.client.KnowledgeRagContextClient;
import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextItem;
import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextRequest;
import com.xcvk.platform.api.contract.knowledge.model.RetrievalMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 上下文召回服务。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagContextRetrievalService {

    /**
     * 双路召回阶段每一路拉取的候选数量。
     */
    private static final int RETRIEVE_STAGE_TOPK = 10;

    /**
     * 查询层 RRF 参数。
     */
    private static final int QUERY_RRF_K = 60;

    private static final String MATCH_TYPE_BOTH = "BOTH";

    private final KnowledgeRagContextClient knowledgeRagContextClient;

    /**
     * 双路增强召回。
     */
    public List<KnowledgeRagContextItem> retrieveEnhancedContexts(String originalQuestion,
                                                                  String rewrittenQuestion,
                                                                  int finalTopK,
                                                                  Long categoryId) {
        List<KnowledgeRagContextItem> originalContexts = retrieveContextsByQuestion(
                originalQuestion,
                RETRIEVE_STAGE_TOPK,
                categoryId,
                RetrievalMode.HYBRID_RRF
        );

        if (!StringUtils.hasText(rewrittenQuestion)
                || safeText(originalQuestion).equals(safeText(rewrittenQuestion))) {
            return mergeAndRerank(originalContexts, List.of(), finalTopK);
        }

        List<KnowledgeRagContextItem> rewrittenContexts = retrieveContextsByQuestion(
                rewrittenQuestion,
                RETRIEVE_STAGE_TOPK,
                categoryId,
                RetrievalMode.HYBRID_RRF
        );

        List<KnowledgeRagContextItem> contexts = mergeAndRerank(
                originalContexts,
                rewrittenContexts,
                finalTopK
        );

        logRetrievalResult(originalQuestion, rewrittenQuestion, originalContexts, rewrittenContexts, contexts);

        return contexts;
    }

    /**
     * 构建回答引用来源。
     */
    public List<RagCitation> buildCitations(List<KnowledgeRagContextItem> contexts) {
        if (CollectionUtils.isEmpty(contexts)) {
            return List.of();
        }

        return contexts.stream()
                .map(RagCitation::from)
                .toList();
    }

    private List<KnowledgeRagContextItem> retrieveContextsByQuestion(
            String question,
            Integer topK,
            Long categoryId,
            RetrievalMode mode
    ) {
        KnowledgeRagContextRequest contextRequest = new KnowledgeRagContextRequest(
                question,
                topK,
                categoryId,
                mode
        );

        return knowledgeRagContextClient.retrieveContexts(contextRequest);
    }

    /**
     * 双路召回。
     */
    public List<KnowledgeRagContextItem> retrieveContextsByMode(
            String originalQuestion,
            String rewrittenQuestion,
            int finalTopK,
            Long categoryId,
            RetrievalMode mode
    ) {
        if (mode == RetrievalMode.ENHANCED_RRF) {
            return retrieveEnhancedContexts(originalQuestion, rewrittenQuestion, finalTopK, categoryId);
        }

        return retrieveContextsByQuestion(originalQuestion, finalTopK, categoryId, mode);
    }

    /**
     * 合并并重排双路召回结果。
     *
     * <p>这里 finalScore 使用查询层 RRF 分数。</p>
     * <p>知识库服务返回的原始 finalScore 只用于同分兜底排序。</p>
     */
    private List<KnowledgeRagContextItem> mergeAndRerank(List<KnowledgeRagContextItem> originalContexts,
                                                         List<KnowledgeRagContextItem> rewrittenContexts,
                                                         int finalTopK) {
        if (CollectionUtils.isEmpty(originalContexts) && CollectionUtils.isEmpty(rewrittenContexts)) {
            return List.of();
        }

        int safeFinalTopK = Math.max(1, finalTopK);

        Map<Long, RerankCandidate> candidateMap = new LinkedHashMap<>();

        addToCandidateMap(candidateMap, originalContexts, true);
        addToCandidateMap(candidateMap, rewrittenContexts, false);

        return candidateMap.values()
                .stream()
                .sorted((left, right) -> {
                    int rrfCompare = Float.compare(right.finalRrfScore(), left.finalRrfScore());
                    if (rrfCompare != 0) {
                        return rrfCompare;
                    }

                    int sourceScoreCompare = Float.compare(right.bestSourceScore, left.bestSourceScore);
                    if (sourceScoreCompare != 0) {
                        return sourceScoreCompare;
                    }

                    int rankCompare = Integer.compare(left.bestRank, right.bestRank);
                    if (rankCompare != 0) {
                        return rankCompare;
                    }

                    return Long.compare(
                            safeChunkId(left.bestItem),
                            safeChunkId(right.bestItem)
                    );
                })
                .limit(safeFinalTopK)
                .map(RerankCandidate::toContextItem)
                .toList();
    }

    private void addToCandidateMap(Map<Long, RerankCandidate> candidateMap,
                                   List<KnowledgeRagContextItem> contexts,
                                   boolean fromOriginalQuestion) {
        if (CollectionUtils.isEmpty(contexts)) {
            return;
        }

        for (int index = 0; index < contexts.size(); index++) {
            KnowledgeRagContextItem context = contexts.get(index);

            if (context == null || context.chunkId() == null) {
                continue;
            }

            int rank = index + 1;

            RerankCandidate candidate = candidateMap.computeIfAbsent(
                    context.chunkId(),
                    ignored -> new RerankCandidate()
            );

            candidate.add(context, rank, fromOriginalQuestion);
        }
    }

    private float queryRrfScore(int rank) {
        return 1.0F / (QUERY_RRF_K + rank);
    }

    private String mergeMatchType(String currentMatchType, String newMatchType) {
        if (!StringUtils.hasText(currentMatchType)) {
            return newMatchType;
        }

        if (!StringUtils.hasText(newMatchType)) {
            return currentMatchType;
        }

        if (MATCH_TYPE_BOTH.equalsIgnoreCase(currentMatchType)
                || MATCH_TYPE_BOTH.equalsIgnoreCase(newMatchType)) {
            return MATCH_TYPE_BOTH;
        }

        if (!currentMatchType.equalsIgnoreCase(newMatchType)) {
            return MATCH_TYPE_BOTH;
        }

        return currentMatchType;
    }

    private void logRetrievalResult(String originalQuestion,
                                    String rewrittenQuestion,
                                    List<KnowledgeRagContextItem> originalContexts,
                                    List<KnowledgeRagContextItem> rewrittenContexts,
                                    List<KnowledgeRagContextItem> finalContexts) {
        log.info("RAG双路检索完成，originalQuestion={}, rewrittenQuestion={}, originalCount={}, rewrittenCount={}, finalCount={}",
                originalQuestion,
                rewrittenQuestion,
                originalContexts.size(),
                rewrittenContexts.size(),
                finalContexts.size()
        );

        for (int i = 0; i < finalContexts.size(); i++) {
            KnowledgeRagContextItem context = finalContexts.get(i);
            log.info("RAG最终片段Top{}，chunkId={}, documentTitle={}, matchType={}, finalScore={}",
                    i + 1,
                    context.chunkId(),
                    context.documentTitle(),
                    context.matchType(),
                    context.finalScore()
            );
        }
    }

    private float safeScore(Float score) {
        return score == null ? 0.0F : score;
    }

    private long safeChunkId(KnowledgeRagContextItem item) {
        if (item == null || item.chunkId() == null) {
            return Long.MAX_VALUE;
        }
        return item.chunkId();
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    /**
     * RRF 候选片段。
     */
    private class RerankCandidate {

        private KnowledgeRagContextItem bestItem;

        private float originalQueryRrfScore;

        private float rewrittenQueryRrfScore;

        /**
         * 知识库服务返回的最高原始分数，只用于同分兜底。
         */
        private float bestSourceScore;

        private int bestRank = Integer.MAX_VALUE;

        private String matchType;

        private void add(KnowledgeRagContextItem item, int rank, boolean fromOriginalQuestion) {
            float currentRrfScore = queryRrfScore(rank);

            if (fromOriginalQuestion) {
                this.originalQueryRrfScore = Math.max(this.originalQueryRrfScore, currentRrfScore);
            } else {
                this.rewrittenQueryRrfScore = Math.max(this.rewrittenQueryRrfScore, currentRrfScore);
            }

            this.bestRank = Math.min(this.bestRank, rank);
            this.matchType = mergeMatchType(this.matchType, item.matchType());

            float sourceScore = safeScore(item.finalScore());

            if (this.bestItem == null || sourceScore > this.bestSourceScore) {
                this.bestItem = item;
                this.bestSourceScore = sourceScore;
            }
        }

        private float finalRrfScore() {
            return originalQueryRrfScore + rewrittenQueryRrfScore;
        }

        private KnowledgeRagContextItem toContextItem() {
            return new KnowledgeRagContextItem(
                    bestItem.chunkId(),
                    bestItem.documentId(),
                    bestItem.chunkNo(),
                    bestItem.documentTitle(),
                    bestItem.categoryName(),
                    bestItem.content(),
                    finalRrfScore(),
                    matchType
            );
        }
    }
}