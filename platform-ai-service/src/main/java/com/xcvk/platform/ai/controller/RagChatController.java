package com.xcvk.platform.ai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.xcvk.platform.ai.model.dto.RagChatRequest;
import com.xcvk.platform.ai.model.request.assistant.AssistantHistoryQueryRequest;
import com.xcvk.platform.ai.model.vo.RagChatResponse;
import com.xcvk.platform.ai.model.vo.assistant.AssistantHistoryItemVO;
import com.xcvk.platform.ai.service.AgentExecutionLogService;
import com.xcvk.platform.ai.service.AssistantService;
import com.xcvk.platform.ai.service.RagChatService;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.auth.starter.util.SaTokenSessionUtils;
import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
    private final SaTokenSessionUtils saTokenSessionUtils;
    private final AgentExecutionLogService executionLogService;

    /**
     * 基于知识库进行问答。
     *
     * @param request RAG 问答请求
     * @return RAG 问答响应
     */
    @PostMapping("/chat")
    @SaCheckLogin
    @AccessLog(value = "RAG知识库问答", recordArgs = false, recordResult = false)
    @Operation(summary = "RAG知识库问答", description = "基于知识库召回内容生成回答")
    public RagChatResponse chat(@Valid @RequestBody RagChatRequest request) {
        return ragChatService.chat(request);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @SaCheckLogin
    @AccessLog(value = "RAG知识库流式问答", recordArgs = false, recordResult = false)
    @Operation(summary = "RAG知识库流式问答", description = "基于知识库召回内容流式生成回答")
    public SseEmitter chatStream(@Valid @RequestBody RagChatRequest request) {
        return ragChatService.chatStream(request);
    }

    @GetMapping("/history")
    @SaCheckLogin
    @AccessLog(value = "查询 AI 历史对话", recordArgs = false, recordResult = false)
    @Operation(summary = "查询 AI 历史对话")
    public Result<PageResult<AssistantHistoryItemVO>> history(
            AssistantHistoryQueryRequest request
    ) {

        CurrentLoginIdentity identity =
                saTokenSessionUtils.getCurrentLoginIdentity();

        return Result.success(executionLogService.queryHistory(identity.userId(), request));
    }
}