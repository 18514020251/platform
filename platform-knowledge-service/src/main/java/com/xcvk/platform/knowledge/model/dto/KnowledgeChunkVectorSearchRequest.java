package com.xcvk.platform.knowledge.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识文档切片向量检索请求
 *
 * <p>用于接收用户问题，并基于问题 embedding 在知识库 chunk 向量索引中检索相似片段。</p>
 *
 * <p>当前阶段主要用于验证向量检索链路，后续可作为 RAG 问答的召回基础。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-28
 */
public record KnowledgeChunkVectorSearchRequest(

        /**
         * 用户问题
         */
        @NotBlank(message = "检索问题不能为空")
        @Size(max = 500, message = "检索问题长度不能超过500个字符")
        String question,

        /**
         * 返回最相似的 chunk 数量
         */
        @Min(value = 1, message = "topK不能小于1")
        @Max(value = 20, message = "topK不能大于20")
        Integer topK,

        /**
         * 分类ID
         *
         * <p>用于限定只在某个知识分类下做向量检索。</p>
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