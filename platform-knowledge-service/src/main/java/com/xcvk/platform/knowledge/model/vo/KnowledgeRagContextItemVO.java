package com.xcvk.platform.knowledge.model.vo;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 知识上下文片段
 *
 * <p>用于将混合检索结果转换为适合大模型 Prompt 使用的干净知识片段。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
public record KnowledgeRagContextItemVO(

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
         * 文档标题
         */
        String documentTitle,

        /**
         * 分类名称
         */
        String categoryName,

        /**
         * 干净的知识片段内容
         *
         * <p>不包含 em 等前端高亮标签，适合放入大模型 Prompt。</p>
         */
        String content,

        /**
         * 融合检索得分
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