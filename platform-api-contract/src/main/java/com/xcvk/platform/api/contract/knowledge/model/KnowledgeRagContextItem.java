package com.xcvk.platform.api.contract.knowledge.model;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 知识上下文片段
 *
 * <p>用于 AI 模块接收知识库模块召回的干净知识片段。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
public record KnowledgeRagContextItem(

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
         * 干净知识片段内容
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