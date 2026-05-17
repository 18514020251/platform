package com.xcvk.platform.api.contract.knowledge.client;

import com.xcvk.platform.api.contract.knowledge.model.KnowledgeDocumentChunkItem;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "platform-knowledge",
        contextId = "platformKnowledgeDocumentChunkClient",
        url = "${platform.remote.knowledge-base-url}"
)
public interface KnowledgeDocumentChunkClient {

    @GetMapping("/internal/documents/{documentId}/chunks")
    List<KnowledgeDocumentChunkItem> listDocumentChunks(
            @PathVariable("documentId") Long documentId
    );
}