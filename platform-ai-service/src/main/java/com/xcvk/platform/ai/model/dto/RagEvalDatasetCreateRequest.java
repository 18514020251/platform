package com.xcvk.platform.ai.model.dto;

import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 新增 RAG 评测样本请求。
 *
 * <p>支持两种方式：
 * 1. 手动传 expectedChunkIds；
 * 2. 传 documentId，由后端远程调用 knowledge-service 自动获取该文档下的 chunkId。</p>
 */
public record RagEvalDatasetCreateRequest(

        @Size(max = 500, message = "问题长度不能超过500个字符")
        String question,

        /**
         * 标准相关 chunk ID。
         *
         * <p>如果该字段为空，但 documentId 不为空，则后端会自动查询该文档所有 chunkId。</p>
         */
        List<Long> expectedChunkIds,

        /**
         * 文档 ID。
         *
         * <p>用于自动获取该文档下的 chunkId，减少手动标注成本。</p>
         */
        Long documentId,

        @Size(max = 2000, message = "期望答案长度不能超过2000个字符")
        String expectedAnswer,

        Long categoryId,

        String difficulty

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}