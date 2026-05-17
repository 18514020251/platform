package com.xcvk.platform.ai.model.vo;

import com.xcvk.platform.ai.model.entity.RagEvalRun;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * RAG 评测任务汇总响应。
 */
public record RagEvalRunVO(

        Long id,

        Integer datasetSize,

        Integer retrieveTopK,

        Long categoryId,

        Boolean generationEnabled,

        Boolean judgeEnabled,

        Double recallAt1,

        Double recallAt3,

        Double recallAt5,

        Double mrr,

        Double contextPrecisionAt5,

        Double avgFaithfulnessScore,

        Double avgRelevanceScore,

        Double avgRetrieveLatencyMs,

        Double avgAnswerLatencyMs,

        LocalDateTime createdAt

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static RagEvalRunVO from(RagEvalRun run) {
        return new RagEvalRunVO(
                run.getId(),
                run.getDatasetSize(),
                run.getRetrieveTopK(),
                run.getCategoryId(),
                run.getGenerationEnabled(),
                run.getJudgeEnabled(),
                run.getRecallAt1(),
                run.getRecallAt3(),
                run.getRecallAt5(),
                run.getMrr(),
                run.getContextPrecisionAt5(),
                run.getAvgFaithfulnessScore(),
                run.getAvgRelevanceScore(),
                run.getAvgRetrieveLatencyMs(),
                run.getAvgAnswerLatencyMs(),
                run.getCreatedAt()
        );
    }
}