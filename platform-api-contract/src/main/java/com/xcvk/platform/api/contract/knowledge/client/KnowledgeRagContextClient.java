package com.xcvk.platform.api.contract.knowledge.client;

import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextItem;
import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 知识库 RAG 上下文远程调用接口
 *
 * <p>用于 AI 模块调用 knowledge-service，召回适合大模型 Prompt 使用的知识片段。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
@FeignClient(
        name = "platform-knowledge",
        contextId = "platformKnowledgeRagContext",
        url = "${platform.remote.knowledge-base-url}"
)
public interface KnowledgeRagContextClient {

    /**
     * 召回 RAG 知识上下文。
     *
     * @param request RAG 知识上下文召回请求
     * @return RAG 知识上下文片段列表
     */
    @PostMapping("/internal/rag/contexts")
    List<KnowledgeRagContextItem> retrieveContexts(@RequestBody KnowledgeRagContextRequest request);
}