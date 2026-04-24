package com.xcvk.platform.knowledge.model.query;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识文档搜索查询条件
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-24
 */
public record KnowledgeDocumentSearchQuery(

        String keyword,

        String status,

        Long categoryId,

        Integer pageNum,

        Integer pageSize

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_PAGE_NUM = 1;

    private static final int DEFAULT_PAGE_SIZE = 10;

    private static final int MAX_PAGE_SIZE = 100;

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