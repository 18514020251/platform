package com.xcvk.platform.ai.controller;

import com.xcvk.platform.ai.model.dto.EmbeddingRequest;
import com.xcvk.platform.ai.model.vo.EmbeddingResponse;
import com.xcvk.platform.ai.service.EmbeddingService;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 文本向量化控制器
 *
 * <p>用于对外提供文本 embedding 能力。
 * 当前阶段主要供 knowledge-service 调用，用于将知识文档 chunk 转换为向量。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-27
 */
@RestController
@RequestMapping("/embeddings")
@RequiredArgsConstructor
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    /**
     * 批量生成文本向量。
     *
     * @param request 文本向量化请求
     * @return 文本向量化响应
     */
    @PostMapping
    @AccessLog(value = "批量生成文本向量", recordArgs = true, recordResult = true)
    @Operation(summary = "批量生成文本向量", description = "批量生成文本向量")
    public Result<EmbeddingResponse> embedTexts(@Valid @RequestBody EmbeddingRequest request) {
        return Result.success(embeddingService.embedTexts(request));
    }
}