package com.xcvk.platform.knowledge.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识文档切片混合检索请求
 *
 * <p>用于接收用户问题，并同时执行 chunk 全文检索和向量检索，
 * 最终融合返回更稳定的知识片段结果。</p>
 *
 * <p>当前阶段主要服务于后续 AI / RAG 问答的知识召回。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
public record KnowledgeChunkHybridSearchRequest(

        /**
         * 用户问题
         *
         * <p>同时作为全文检索关键词和向量检索输入。</p>
         */
        @NotBlank(message = "检索问题不能为空")
        @Size(max = 500, message = "检索问题长度不能超过500个字符")
        String question,

        /**
         * 返回结果数量
         */
        @Min(value = 1, message = "topK不能小于1")
        @Max(value = 20, message = "topK不能大于20")
        Integer topK,

        /**
         * 分类ID
         *
         * <p>用于限定在某个知识分类下做混合检索。</p>
         */
        Long categoryId

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_TOP_K = 5;

    private static final int MAX_TOP_K = 20;

    public int safeTopK() {
        if (topK == null || topK < 1) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }
}