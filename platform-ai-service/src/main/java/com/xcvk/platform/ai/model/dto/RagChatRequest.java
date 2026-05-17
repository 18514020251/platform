package com.xcvk.platform.ai.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 问答请求
 *
 * <p>用于接收用户问题，并基于知识库召回内容生成回答。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
public record RagChatRequest(

        /**
         * 用户问题
         */
        @NotBlank(message = "问题不能为空")
        @Size(max = 500, message = "问题长度不能超过500个字符")
        String question,

        /**
         * 召回知识片段数量
         */
        @Min(value = 1, message = "topK不能小于1")
        @Max(value = 10, message = "topK不能大于10")
        Integer topK,

        /**
         * 分类ID
         */
        Long categoryId,

        Long executionLogId

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_TOP_K = 5;

    private static final int MAX_TOP_K = 10;

    public int safeTopK() {
        if (topK == null || topK < 1) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }
}