package com.xcvk.platform.ai.model.vo;

import com.xcvk.platform.ai.model.entity.RagEvalCaseResult;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG 单样本评测结果响应。
 */
public record RagEvalCaseResultVO(

        Long id,

        Long runId,

        Long datasetId,

        String question,

        List<Long> expectedChunkIds,

        List<Long> retrievedChunkIds,

        Boolean hitAt1,

        Boolean hitAt3,

        Boolean hitAt5,

        Double reciprocalRank,

        Double contextPrecisionAt5,

        String answer,

        Double faithfulnessScore,

        Double relevanceScore,

        Long retrieveLatencyMs,

        Long answerLatencyMs,

        LocalDateTime createdAt

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static RagEvalCaseResultVO from(RagEvalCaseResult result,
                                           List<Long> expectedChunkIds,
                                           List<Long> retrievedChunkIds) {
        return new RagEvalCaseResultVO(
                result.getId(),
                result.getRunId(),
                result.getDatasetId(),
                result.getQuestion(),
                expectedChunkIds,
                retrievedChunkIds,
                result.getHitAt1(),
                result.getHitAt3(),
                result.getHitAt5(),
                result.getReciprocalRank(),
                result.getContextPrecisionAt5(),
                result.getAnswer(),
                result.getFaithfulnessScore(),
                result.getRelevanceScore(),
                result.getRetrieveLatencyMs(),
                result.getAnswerLatencyMs(),
                result.getCreatedAt()
        );
    }
}