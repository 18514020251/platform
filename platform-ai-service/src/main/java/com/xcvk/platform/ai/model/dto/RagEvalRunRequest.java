package com.xcvk.platform.ai.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 离线评测执行请求。
 */
public record RagEvalRunRequest(

        Long categoryId,

        @Min(value = 1, message = "datasetLimit不能小于1")
        @Max(value = 200, message = "datasetLimit不能大于200")
        Integer datasetLimit,

        @Min(value = 1, message = "retrieveTopK不能小于1")
        @Max(value = 10, message = "retrieveTopK不能大于10")
        Integer retrieveTopK,

        Boolean generationEnabled,

        Boolean judgeEnabled

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_DATASET_LIMIT = 50;

    private static final int MAX_DATASET_LIMIT = 200;

    private static final int DEFAULT_RETRIEVE_TOP_K = 5;

    private static final int MAX_RETRIEVE_TOP_K = 10;

    public int safeDatasetLimit() {
        if (datasetLimit == null || datasetLimit < 1) {
            return DEFAULT_DATASET_LIMIT;
        }
        return Math.min(datasetLimit, MAX_DATASET_LIMIT);
    }

    public int safeRetrieveTopK() {
        if (retrieveTopK == null || retrieveTopK < 1) {
            return DEFAULT_RETRIEVE_TOP_K;
        }
        return Math.min(retrieveTopK, MAX_RETRIEVE_TOP_K);
    }

    public boolean safeGenerationEnabled() {
        return Boolean.TRUE.equals(generationEnabled) || Boolean.TRUE.equals(judgeEnabled);
    }

    public boolean safeJudgeEnabled() {
        return Boolean.TRUE.equals(judgeEnabled);
    }
}