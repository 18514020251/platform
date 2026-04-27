package com.xcvk.platform.knowledge.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识文档切片搜索列表项
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-27
 */
public record KnowledgeChunkSearchItemVO(

        Long chunkId,

        Long documentId,

        Integer chunkNo,

        String chunkText,

        String documentTitle,

        Long categoryId,

        String categoryName,

        String tags,

        String status,

        Integer tokenCount,

        LocalDateTime updatedAt

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}