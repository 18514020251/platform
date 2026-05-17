package com.xcvk.platform.api.contract.knowledge.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识文档切片项。
 *
 * <p>用于服务间调用时返回某篇文档下的 chunk 列表。</p>
 */
public record KnowledgeDocumentChunkItem(

        /**
         * chunk ID。
         */
        Long id,

        /**
         * 文档 ID。
         */
        Long documentId,

        /**
         * 切片序号。
         */
        Integer chunkNo,

        /**
         * 切片文本。
         */
        String chunkText,

        /**
         * 切片 hash。
         */
        String chunkHash,

        /**
         * token 数。
         */
        Integer tokenCount,

        /**
         * 状态。
         */
        String status,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}