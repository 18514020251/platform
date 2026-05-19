package com.xcvk.platform.knowledge.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识文档详情。
 *
 * <p>用于文档管理页编辑/查看，包含列表接口不会返回的正文 content。</p>
 */
public record KnowledgeDocumentDetailVO(

        Long documentId,

        String title,

        String summary,

        String content,

        Long categoryId,

        String categoryName,

        String tags,

        String status,

        Long creatorId,

        String creatorName,

        LocalDateTime publishedAt,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
