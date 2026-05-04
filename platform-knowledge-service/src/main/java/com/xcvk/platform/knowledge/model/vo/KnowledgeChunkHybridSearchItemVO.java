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
         * 全文检索排名分
         *
         * <p>当前阶段使用 rank-based score，第1名为1.0，第2名为0.5。</p>
         */
        Float textRankScore,

        /**
         * 向量检索排名分
         *
         * <p>当前阶段使用 rank-based score，第1名为1.0，第2名为0.5。</p>
         */
        Float vectorRankScore,

        /**
         * 向量检索原始分
         *
         * <p>该字段来自 Elasticsearch script_score 计算结果，
         * 用于观察向量相似度，不直接作为融合分数使用。</p>
         */
        Float vectorRawScore,

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