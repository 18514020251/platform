package com.xcvk.platform.knowledge.search.assembler;

import com.xcvk.platform.knowledge.model.entity.KnowledgeDocument;
import com.xcvk.platform.knowledge.search.model.index.KnowledgeDocumentIndex;
import org.springframework.stereotype.Component;

/**
 * 知识文档搜索转换器
 *
 * <p>用于将 MySQL 中的知识文档业务实体转换为 Elasticsearch 搜索索引对象，
 * 避免搜索字段转换逻辑散落在业务 Service 中。</p>
 *
 * @author Programmer
 * @since 2026-04-24
 */
@Component
public class KnowledgeSearchAssembler {

    /**
     * 将知识文档实体转换为 ES 索引对象
     *
     * @param document 知识文档实体
     * @return 知识文档 ES 索引对象
     */
    public KnowledgeDocumentIndex toIndex(KnowledgeDocument document) {
        if (document == null) {
            return null;
        }

        return new KnowledgeDocumentIndex()
                .setId(document.getId())
                .setDocumentId(document.getId())
                .setTitle(document.getTitle())
                .setSummary(document.getSummary())
                .setContent(document.getContent())
                .setCategoryId(document.getCategoryId())
                .setCategoryName(document.getCategoryName())
                .setTags(document.getTags())
                .setStatus(document.getStatus())
                .setCreatorId(document.getCreatorId())
                .setCreatorName(document.getCreatorName())
                .setPublishedAt(document.getPublishedAt())
                .setCreatedAt(document.getCreatedAt())
                .setUpdatedAt(document.getUpdatedAt());
    }
}