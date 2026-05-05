package com.xcvk.platform.knowledge.controller;

import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextItem;
import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextRequest;
import com.xcvk.platform.knowledge.model.dto.KnowledgeChunkHybridSearchRequest;
import com.xcvk.platform.knowledge.model.vo.KnowledgeRagContextItemVO;
import com.xcvk.platform.knowledge.service.KnowledgeRagContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库 RAG 内部接口
 *
 * <p>用于服务间调用，当前主要供 ai-service 获取适合大模型 Prompt 使用的知识上下文。</p>
 *
 * <p>该接口直接返回业务数据，不包装 Result，避免内部调用方再解析外部响应结构。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
@RestController
@RequestMapping("/internal/rag")
@RequiredArgsConstructor
public class KnowledgeRagInternalController {

    private final KnowledgeRagContextService knowledgeRagContextService;

    /**
     * 召回 RAG 知识上下文。
     *
     * @param request RAG 知识上下文召回请求
     * @return RAG 知识上下文片段列表
     */
    @PostMapping("/contexts")
    public List<KnowledgeRagContextItem> retrieveContexts(@RequestBody KnowledgeRagContextRequest request) {
        KnowledgeChunkHybridSearchRequest searchRequest = new KnowledgeChunkHybridSearchRequest(
                request.question(),
                request.topK(),
                request.categoryId()
        );

        return knowledgeRagContextService.retrieveContexts(searchRequest)
                .stream()
                .map(this::toContractItem)
                .toList();
    }

    /**
     * 将 knowledge-service 内部 VO 转换为 api-contract 对象。
     *
     * @param item RAG 上下文 VO
     * @return RAG 上下文契约对象
     */
    private KnowledgeRagContextItem toContractItem(KnowledgeRagContextItemVO item) {
        return new KnowledgeRagContextItem(
                item.chunkId(),
                item.documentId(),
                item.chunkNo(),
                item.documentTitle(),
                item.categoryName(),
                item.content(),
                item.finalScore(),
                item.matchType()
        );
    }
}