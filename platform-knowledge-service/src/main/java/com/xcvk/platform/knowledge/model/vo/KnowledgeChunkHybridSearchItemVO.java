package com.xcvk.platform.knowledge.model.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * 知识文档切片混合检索结果项
 *
 * <p>用于返回全文检索和向量检索融合后的知识片段。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
public record KnowledgeChunkHybridSearchItemVO(

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
         * 全文检索得分
         */
        Float textScore,

        /**
         * 向量检索得分
         */
        Float vectorScore,

        /**
         * 融合后的最终得分
         */
        Float finalScore,

        /**
         * 命中类型：TEXT / VECTOR / BOTH
         */
        String matchType

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}