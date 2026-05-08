package com.xcvk.platform.ai.service.impl;

import com.xcvk.platform.ai.model.dto.RagChatRequest;
import com.xcvk.platform.ai.model.vo.RagChatResponse;
import com.xcvk.platform.ai.model.vo.RagCitation;
import com.xcvk.platform.ai.service.RagChatService;
import com.xcvk.platform.api.contract.knowledge.client.KnowledgeRagContextClient;
import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextItem;
import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextRequest;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

/**
 * RAG 问答服务实现类
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-08
 */
@Service
@RequiredArgsConstructor
public class RagChatServiceImpl implements RagChatService {


    private static final String SSE_EVENT_CONTEXTS = "contexts";

    private static final String SSE_EVENT_DELTA = "delta";

    private static final String SSE_EVENT_DONE = "done";

    private static final String SSE_EVENT_ERROR = "error";

    private static final String SSE_EVENT_REJECTED = "rejected";

    private static final String SSE_DONE_FLAG = "[DONE]";

    private static final long SSE_TIMEOUT_MS = 180_000L;

    private static final String NO_CONTEXT_ANSWER = "知识库中暂未检索到相关内容，无法基于现有知识库回答该问题。";

    private static final String WEAK_CONTEXT_ANSWER = "当前知识库未提供足够可靠的依据，暂时无法回答该问题。";

    private static final String REJECT_REASON_NO_CONTEXT = "NO_CONTEXT";

    private static final String REJECT_REASON_LOW_RELEVANCE = "LOW_RELEVANCE";

    /**
     * RRF 融合后，第一名单路召回得分约为 1 / 61 = 0.01639。
     */
    private static final float MIN_ACCEPTED_CONTEXT_SCORE = 0.016F;

    private static final String PROMPT_SYSTEM_ROLE = """
        你是企业知识库助手，请严格基于【知识库内容】回答用户问题。
        如果知识库内容不足以回答，请明确说明无法从当前知识库中确定，不要编造。
        """;

    private static final String PROMPT_KNOWLEDGE_BASE_TITLE = "【知识库内容】\n";

    private static final String PROMPT_SECTION_TEMPLATE = """
        片段{index}：
        文档标题：{documentTitle}
        分类：{categoryName}
        内容：
        {content}
        
        """;

    private static final String PROMPT_QUESTION_TITLE = "【用户问题】\n";

    private static final String PROMPT_REQUIREMENTS_TITLE = "【回答要求】\n";

    private static final String PROMPT_REQUIREMENTS_CONTENT = """
        1. 只基于知识库内容回答，不要编造。
        2. 如果知识库片段中已经出现相关章节、条款或明确描述，不要声称知识库未明确说明。
        3. 回答要简洁、清晰，优先使用条目化列表。
        4. 回答末尾请标注依据的片段编号，例如：依据：片段2、片段3。
        5. 如果知识库内容确实不足，请只说明“当前知识库未提供足够信息”，不要扩展推测。
        6. 如果多个片段都与问题相关，请综合所有相关片段，不要只回答其中一个片段中的部分条目。
        """;


    private final Object emitterLock = new Object();

    private final KnowledgeRagContextClient knowledgeRagContextClient;
    private final ChatModel chatModel;
    private final StreamingChatModel streamingChatModel;

    @Override
    public RagChatResponse chat(RagChatRequest request) {
        validateRequest(request);

        List<KnowledgeRagContextItem> contexts = retrieveContexts(request);
        List<RagCitation> citations = buildCitations(contexts);

        if (CollectionUtils.isEmpty(contexts)) {
            return RagChatResponse.rejected(NO_CONTEXT_ANSWER, REJECT_REASON_NO_CONTEXT, citations);
        }

        if (isLowRelevance(contexts)) {
            return RagChatResponse.rejected(WEAK_CONTEXT_ANSWER, REJECT_REASON_LOW_RELEVANCE, citations);
        }

        String prompt = buildPrompt(request.question(), contexts);
        String answer = chatModel.chat(prompt);

        return RagChatResponse.answered(answer, citations);
    }

    @Override
    public SseEmitter chatStream(RagChatRequest request) {
        validateRequest(request);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        List<KnowledgeRagContextItem> contexts = retrieveContexts(request);
        List<RagCitation> citations = buildCitations(contexts);
        sendEvent(emitter, SSE_EVENT_CONTEXTS, citations);

        if (CollectionUtils.isEmpty(contexts)) {
            sendRejectEvents(emitter, NO_CONTEXT_ANSWER, REJECT_REASON_NO_CONTEXT);
            return emitter;
        }

        if (isLowRelevance(contexts)) {
            sendRejectEvents(emitter, WEAK_CONTEXT_ANSWER, REJECT_REASON_LOW_RELEVANCE);
            return emitter;
        }

        String prompt = buildPrompt(request.question(), contexts);

        try {
            streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {

                @Override
                public void onPartialResponse(String partialResponse) {
                    sendEvent(emitter, SSE_EVENT_DELTA, partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    sendEvent(emitter, SSE_EVENT_DONE, SSE_DONE_FLAG);
                    emitter.complete();
                }

                @Override
                public void onError(Throwable error) {
                    sendEvent(emitter, SSE_EVENT_ERROR, error.getMessage());
                    emitter.completeWithError(error);
                }
            });
        } catch (Exception ex) {
            sendEvent(emitter, SSE_EVENT_ERROR, ex.getMessage());
            emitter.completeWithError(ex);
        }

        return emitter;
    }

    /**
     * 召回相关知识片段
     */
    private List<KnowledgeRagContextItem> retrieveContexts(RagChatRequest request) {
        KnowledgeRagContextRequest contextRequest = new KnowledgeRagContextRequest(
                request.question(),
                request.safeTopK(),
                request.categoryId()
        );

        List<KnowledgeRagContextItem> contexts = knowledgeRagContextClient.retrieveContexts(contextRequest);
        return contexts == null ? List.of() : contexts;
    }

    /**
     * 构建回答引用来源。
     */
    private List<RagCitation> buildCitations(List<KnowledgeRagContextItem> contexts) {
        if (CollectionUtils.isEmpty(contexts)) {
            return List.of();
        }

        return contexts.stream()
                .map(RagCitation::from)
                .toList();
    }

    /**
     * 判断召回结果是否低相关。
     */
    private boolean isLowRelevance(List<KnowledgeRagContextItem> contexts) {
        if (CollectionUtils.isEmpty(contexts)) {
            return true;
        }

        return contexts.stream()
                .map(KnowledgeRagContextItem::finalScore)
                .filter(score -> score != null)
                .max(Float::compareTo)
                .map(maxScore -> maxScore < MIN_ACCEPTED_CONTEXT_SCORE)
                .orElse(true);
    }

    /**
     * 构建 Prompt
     */
    private String buildPrompt(String question, List<KnowledgeRagContextItem> contexts) {
        StringBuilder builder = new StringBuilder();

        builder.append(PROMPT_SYSTEM_ROLE).append("\n");

        builder.append(PROMPT_KNOWLEDGE_BASE_TITLE);
        for (int i = 0; i < contexts.size(); i++) {
            KnowledgeRagContextItem context = contexts.get(i);
            builder.append(PROMPT_SECTION_TEMPLATE
                    .replace("{index}", String.valueOf(i + 1))
                    .replace("{documentTitle}", safeText(context.documentTitle()))
                    .replace("{categoryName}", safeText(context.categoryName()))
                    .replace("{content}", safeText(context.content())));
        }

        builder.append(PROMPT_QUESTION_TITLE);
        builder.append(question.trim()).append("\n\n");

        builder.append(PROMPT_REQUIREMENTS_TITLE);
        builder.append(PROMPT_REQUIREMENTS_CONTENT);

        return builder.toString();
    }

    /**
     * 校验请求参数
     */
    private void validateRequest(RagChatRequest request) {
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, "RAG问答请求不能为空");
        BizAssert.hasText(request.question(), ErrorCode.PARAM_INVALID, "问题不能为空");
    }

    /**
     * 安全获取文本，null 转空字符串并去除首尾空格
     */
    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    /**
     * 发送 SSE 拒答事件。
     */
    private void sendRejectEvents(SseEmitter emitter, String answer, String rejectReason) {
        sendEvent(emitter, SSE_EVENT_REJECTED, rejectReason);
        sendEvent(emitter, SSE_EVENT_DELTA, answer);
        sendEvent(emitter, SSE_EVENT_DONE, SSE_DONE_FLAG);
        emitter.complete();
    }

    /**
     * 发送 SSE 事件
     */
    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        synchronized (emitterLock) {
            try {
                emitter.send(
                        SseEmitter.event()
                                .name(eventName)
                                .data(data)
                );
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }
    }
}