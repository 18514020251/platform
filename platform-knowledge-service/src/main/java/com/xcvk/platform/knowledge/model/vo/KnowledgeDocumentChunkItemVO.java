package com.xcvk.platform.knowledge.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识文档切片列表项
 *
 * <p>用于开发阶段查看某篇知识文档的切片结果，
 * 方便排查切片质量、chunk 顺序和后续向量化结果。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-26
 */
public record KnowledgeDocumentChunkItemVO(

        Long id,

        Long documentId,

        Integer chunkNo,

        String chunkText,

        String chunkHash,

        Integer tokenCount,

        String status,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}