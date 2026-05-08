package com.xcvk.platform.ai.model.vo;

import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextItem;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;

/**
 * RAG 回答引用来源。
 *
 * <p>面向前端展示和答案溯源，只暴露必要的文档、切片、匹配和预览信息，
 * 避免把完整上下文直接作为回答结构返回。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-08
 */
public record RagCitation(

        /**
         * chunk ID
         */
        Long chunkId,

        /**
         * 知识文档ID
         */
        Long documentId,

        /**
         * 文档标题
         */
        String documentTitle,

        /**
         * 切片序号
         */
        Integer chunkNo,

        /**
         * 分类名称
         */
        String categoryName,

        /**
         * 引用片段内容预览
         */
        String contentPreview,

        /**
         * 融合检索得分
         */
        Float score,

        /**
         * 命中类型：TEXT / VECTOR / BOTH
         */
        String matchType

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_PREVIEW_LENGTH = 160;

    public static RagCitation from(KnowledgeRagContextItem item) {
        return new RagCitation(
                item.chunkId(),
                item.documentId(),
                item.documentTitle(),
                item.chunkNo(),
                item.categoryName(),
                buildPreview(item.content()),
                item.finalScore(),
                item.matchType()
        );
    }

    private static String buildPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }

        String normalized = content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= DEFAULT_PREVIEW_LENGTH) {
            return normalized;
        }

        return normalized.substring(0, DEFAULT_PREVIEW_LENGTH) + "...";
    }
}