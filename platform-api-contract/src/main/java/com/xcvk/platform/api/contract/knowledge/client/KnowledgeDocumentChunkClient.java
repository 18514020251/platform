package com.xcvk.platform.api.contract.knowledge.client;

import com.xcvk.platform.api.contract.knowledge.model.KnowledgeDocumentChunkItem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 知识文档切片远程调用接口。
 *
 * <p>用于 ai-service 在构建 RAG 评测样本时，根据 documentId 自动获取 chunkId。</p>
 */
@FeignClient(
        name = "platform-knowledge",
        contextId = "platformKnowledgeDocumentChunkClient",
        url = "${platform.remote.knowledge-base-url}"
)
public interface KnowledgeDocumentChunkClient {

    /**
     * 查询指定知识文档的切片列表。
     *
     * @param documentId 知识文档 ID
     * @return 切片列表
     */
    @GetMapping("/internal/documents/{documentId}/chunks")
    List<KnowledgeDocumentChunkItem> listDocumentChunks(@PathVariable("documentId") Long documentId);
}