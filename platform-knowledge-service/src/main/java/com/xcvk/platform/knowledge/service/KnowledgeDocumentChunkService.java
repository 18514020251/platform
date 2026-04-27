package com.xcvk.platform.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcvk.platform.knowledge.model.entity.KnowledgeDocument;
import com.xcvk.platform.knowledge.model.entity.KnowledgeDocumentChunk;

/**
 * 知识文档切片服务接口
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-26
 */
public interface KnowledgeDocumentChunkService extends IService<KnowledgeDocumentChunk> {

    /**
     * 重建知识文档切片。
     *
     * <p>用于创建或更新知识文档后，根据最新正文重新生成切片数据。</p>
     *
     * @param document 知识文档实体
     */
    void rebuildDocumentChunks(KnowledgeDocument document);

    /**
     * 下线知识文档切片。
     *
     * <p>用于知识文档下线后，将其对应切片统一标记为下线。</p>
     *
     * @param document 知识文档实体
     */
    void offlineDocumentChunks(KnowledgeDocument document);
}