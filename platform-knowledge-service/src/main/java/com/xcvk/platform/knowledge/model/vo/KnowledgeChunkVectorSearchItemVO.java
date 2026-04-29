package com.xcvk.platform.knowledge.model.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识文档切片向量检索结果项
 *
 * <p>用于返回 ES kNN 检索命中的相似知识片段。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-28
 */
public record KnowledgeChunkVectorSearchItemVO(

        /**
         * chunk ID
         */
        Long chunkId,

        /**
         * 知识文档ID
         */
        Long documentId,

        /**
         * 切片序号
         */
        Integer chunkNo,

        /**
         * 切片文本
         */
        String chunkText,

        /**
         * 文档标题
         */
        String documentTitle,

        /**
         * 分类ID
         */
        Long categoryId,

        /**
         * 分类名称
         */
        String categoryName,

        /**
         * 标签
         */
        String tags,

        /**
         * ES 相似度得分
         */
        Float score

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}