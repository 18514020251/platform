package com.xcvk.platform.knowledge.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识文档搜索列表项
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-24
 */
public record KnowledgeDocumentSearchItemVO(

        Long documentId,

        String title,

        String summary,

        Long categoryId,

        String categoryName,

        String tags,

        String status,

        String creatorName,

        LocalDateTime publishedAt,

        LocalDateTime updatedAt

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}