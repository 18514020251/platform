package com.xcvk.platform.knowledge.search.assembler;

import com.xcvk.platform.knowledge.model.entity.KnowledgeDocument;
import com.xcvk.platform.knowledge.model.entity.KnowledgeDocumentChunk;
import com.xcvk.platform.knowledge.search.model.index.KnowledgeChunkIndex;
import org.springframework.stereotype.Component;

/**
 * 知识文档切片搜索转换器
 *
 * <p>用于将 MySQL 中的知识文档切片实体转换为 Elasticsearch 切片索引对象，
 * 避免搜索索引组装逻辑散落在业务 Service 中。</p>
 *
 * <p>chunk 索引会冗余部分文档信息，如标题、分类、标签等，
 * 方便后续 RAG 检索结果直接展示来源信息。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-26
 */
@Component
public class KnowledgeChunkSearchAssembler {

    /**
     * 将知识文档切片转换为 ES 切片索引对象。
     *
     * @param document 知识文档实体
     * @param chunk 知识文档切片实体
     * @return 知识文档切片 ES 索引对象
     */
    public KnowledgeChunkIndex toIndex(KnowledgeDocument document, KnowledgeDocumentChunk chunk) {
        if (document == null || chunk == null) {
            return null;
        }

        return new KnowledgeChunkIndex()
                .setId(chunk.getId())
                .setDocumentId(chunk.getDocumentId())
                .setChunkNo(chunk.getChunkNo())
                .setChunkText(chunk.getChunkText())
                .setChunkHash(chunk.getChunkHash())
                .setTokenCount(chunk.getTokenCount())
                .setStatus(chunk.getStatus())
                .setDocumentTitle(document.getTitle())
                .setCategoryId(document.getCategoryId())
                .setCategoryName(document.getCategoryName())
                .setTags(document.getTags())
                .setCreatedAt(chunk.getCreatedAt())
                .setUpdatedAt(chunk.getUpdatedAt());
    }
}