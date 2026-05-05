package com.xcvk.platform.ai.controller;

import com.xcvk.platform.ai.model.dto.RagChatRequest;
import com.xcvk.platform.ai.model.vo.RagChatResponse;
import com.xcvk.platform.ai.service.RagChatService;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * RAG 问答控制器
 *
 * <p>用于接收用户问题，基于知识库召回内容生成回答。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagChatController {

    private final RagChatService ragChatService;

    /**
     * 基于知识库进行问答。
     *
     * @param request RAG 问答请求
     * @return RAG 问答响应
     */
    @PostMapping("/chat")
    @AccessLog(value = "RAG知识库问答", recordArgs = false, recordResult = false)
    @Operation(summary = "RAG知识库问答", description = "基于知识库召回内容生成回答")
    public RagChatResponse chat(@Valid @RequestBody RagChatRequest request) {
        return ragChatService.chat(request);
    }
}