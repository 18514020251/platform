package com.xcvk.platform.knowledge.model.query;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识文档切片搜索查询条件
 *
 * <p>用于 chunk 级全文检索，当前阶段主要服务于开发验证和后续 AI / RAG 检索。</p>
 *
 * <p>后续接入向量检索后，可继续扩展 queryVector、topK、minScore 等参数，
 * 但当前阶段先保持简单，只做关键词检索和基础过滤。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-27
 */
public record KnowledgeChunkSearchQuery(

        /**
         * 搜索关键词
         */
        String keyword,

        /**
         * 知识文档ID
         */
        Long documentId,

        /**
         * 分类ID
         */
        Long categoryId,

        /**
         * 切片状态
         */
        String status,

        /**
         * 页码
         */
        Integer pageNum,

        /**
         * 每页大小
         */
        Integer pageSize

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_PAGE_NUM = 1;

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final int MAX_PAGE_SIZE = 50;

    public int safePageNum() {
        return pageNum == null || pageNum < 1 ? DEFAULT_PAGE_NUM : pageNum;
    }

    public int safePageSize() {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }
}