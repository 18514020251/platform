package com.xcvk.platform.ai.model.vo;

import com.xcvk.platform.ai.model.entity.RagEvalDataset;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG 评测样本响应。
 */
public record RagEvalDatasetVO(

        Long id,

        String question,

        List<Long> expectedChunkIds,

        String expectedAnswer,

        Long categoryId,

        String difficulty,

        LocalDateTime createdAt

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static RagEvalDatasetVO from(RagEvalDataset dataset, List<Long> expectedChunkIds) {
        return new RagEvalDatasetVO(
                dataset.getId(),
                dataset.getQuestion(),
                expectedChunkIds,
                dataset.getExpectedAnswer(),
                dataset.getCategoryId(),
                dataset.getDifficulty(),
                dataset.getCreatedAt()
        );
    }
}