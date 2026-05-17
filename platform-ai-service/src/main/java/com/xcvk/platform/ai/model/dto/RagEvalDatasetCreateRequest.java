package com.xcvk.platform.ai.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 新增 RAG 评测样本请求。
 */
public record RagEvalDatasetCreateRequest(

        @NotBlank(message = "问题不能为空")
        @Size(max = 500, message = "问题长度不能超过500个字符")
        String question,

        @NotEmpty(message = "标准chunk不能为空")
        List<Long> expectedChunkIds,

        @Size(max = 2000, message = "期望答案长度不能超过2000个字符")
        String expectedAnswer,

        Long categoryId,

        String difficulty

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}