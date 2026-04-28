package com.xcvk.platform.api.contract.ai.client;

import com.xcvk.platform.api.contract.ai.model.EmbeddingRequest;
import com.xcvk.platform.api.contract.ai.model.EmbeddingResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * AI 模块远程调用接口
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-28
 */
@FeignClient(
        name = "platform-ai",
        contextId = "platformAiEmbeddingClient",
        url = "${platform.remote.ai-base-url}"
)
public interface EmbedClient {

    /**
     * 批量生成文本向量。
     *
     * @param request 文本向量化请求
     * @return 文本向量化响应
     */
    @PostMapping("/embeddings")
    EmbeddingResponse embedTexts(@RequestBody EmbeddingRequest request);
}