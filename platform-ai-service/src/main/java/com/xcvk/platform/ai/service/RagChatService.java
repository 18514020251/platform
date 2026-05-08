package com.xcvk.platform.ai.service;

import com.xcvk.platform.ai.model.dto.RagChatRequest;
import com.xcvk.platform.ai.model.vo.RagChatResponse;
import dev.langchain4j.service.spring.AiService;
import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * RAG 问答服务
 *
 * <p>用于基于知识库召回内容生成回答。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
public interface RagChatService {

    /**
     * 基于知识库进行问答。
     *
     * @param request RAG 问答请求
     * @return RAG 问答响应
     */
    RagChatResponse chat(RagChatRequest request);

    SseEmitter chatStream(@Valid RagChatRequest request);
}