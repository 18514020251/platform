package com.xcvk.platform.knowledge.controller.internal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xcvk.platform.api.contract.knowledge.model.KnowledgeDocumentChunkItem;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.knowledge.constant.KnowledgeErrorMessages;
import com.xcvk.platform.knowledge.model.entity.KnowledgeDocumentChunk;
import com.xcvk.platform.knowledge.service.KnowledgeDocumentChunkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/documents")
@RequiredArgsConstructor
public class KnowledgeDocumentInternalController {

    private final KnowledgeDocumentChunkService knowledgeDocumentChunkService;

    @GetMapping("/{documentId}/chunks")
    public List<KnowledgeDocumentChunkItem> listDocumentChunks(
            @PathVariable("documentId") Long documentId
    ) {
        BizAssert.notNull(
                documentId,
                ErrorCode.PARAM_INVALID,
                KnowledgeErrorMessages.DOCUMENT_ID_REQUIRED
        );

        return knowledgeDocumentChunkService.list(
                        new LambdaQueryWrapper<KnowledgeDocumentChunk>()
                                .eq(KnowledgeDocumentChunk::getDocumentId, documentId)
                                .orderByAsc(KnowledgeDocumentChunk::getChunkNo)
                )
                .stream()
                .map(this::toContractItem)
                .toList();
    }

    private KnowledgeDocumentChunkItem toContractItem(KnowledgeDocumentChunk chunk) {
        return new KnowledgeDocumentChunkItem(
                chunk.getId(),
                chunk.getDocumentId(),
                chunk.getChunkNo(),
                chunk.getChunkText(),
                chunk.getChunkHash(),
                chunk.getTokenCount(),
                chunk.getStatus(),
                chunk.getCreatedAt(),
                chunk.getUpdatedAt()
        );
    }
}