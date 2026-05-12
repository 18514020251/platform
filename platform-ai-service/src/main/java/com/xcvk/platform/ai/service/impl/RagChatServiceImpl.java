package com.xcvk.platform.ai.service.impl;

import com.xcvk.platform.ai.model.dto.RagChatRequest;
import com.xcvk.platform.ai.model.vo.RagChatResponse;
import com.xcvk.platform.ai.model.vo.RagCitation;
import com.xcvk.platform.ai.service.RagChatService;
import com.xcvk.platform.ai.service.rag.RagContextRetrievalService;
import com.xcvk.platform.ai.service.rag.RagPromptBuilder;
import com.xcvk.platform.ai.service.rag.RagQuestionRewriteService;
import com.xcvk.platform.ai.service.rag.RagRelevanceEvaluator;
import com.xcvk.platform.ai.service.rag.RagSseEmitterSender;
import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextItem;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * RAG 问答服务实现类。
 *
 * <p>这里只保留问答主流程编排，不再承载问题改写、召回、Prompt 构建、SSE 发送等细节。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagChatServiceImpl implements RagChatService {

    private static final String NO_CONTEXT_ANSWER = "知识库中暂未检索到相关内容，无法基于现有知识库回答该问题。";

    private static final String WEAK_CONTEXT_ANSWER = "当前知识库未提供足够可靠的依据，暂时无法回答该问题。";

    private static final String REJECT_REASON_NO_CONTEXT = "NO_CONTEXT";

    private static final String REJECT_REASON_LOW_RELEVANCE = "LOW_RELEVANCE";

    private final ChatModel chatModel;

    private final StreamingChatModel streamingChatModel;

    private final RagQuestionRewriteService questionRewriteService;

    private final RagContextRetrievalService contextRetrievalService;

    private final RagPromptBuilder promptBuilder;

    private final RagRelevanceEvaluator relevanceEvaluator;

    private final RagSseEmitterSender sseEmitterSender;

    @Override
    public RagChatResponse chat(RagChatRequest request) {
        validateRequest(request);

        String originalQuestion = request.question();
        String rewrittenQuestion = questionRewriteService.rewriteQuestion(originalQuestion);

        List<KnowledgeRagContextItem> contexts = contextRetrievalService.retrieveEnhancedContexts(
                originalQuestion,
                rewrittenQuestion,
                request.safeTopK(),
                request.categoryId()
        );

        List<RagCitation> citations = contextRetrievalService.buildCitations(contexts);

        if (contexts.isEmpty()) {
            return RagChatResponse.rejected(NO_CONTEXT_ANSWER, REJECT_REASON_NO_CONTEXT, citations);
        }

        if (relevanceEvaluator.isLowRelevance(contexts)) {
            return RagChatResponse.rejected(WEAK_CONTEXT_ANSWER, REJECT_REASON_LOW_RELEVANCE, citations);
        }

        String prompt = promptBuilder.buildEnhancedAnswerPrompt(
                originalQuestion,
                rewrittenQuestion,
                contexts
        );

        String answer = chatModel.chat(prompt);

        return RagChatResponse.answered(answer, citations);
    }

    @Override
    public SseEmitter chatStream(RagChatRequest request) {
        validateRequest(request);

        SseEmitter emitter = sseEmitterSender.createEmitter();

        String originalQuestion = request.question();
        String rewrittenQuestion = questionRewriteService.rewriteQuestion(originalQuestion);

        List<KnowledgeRagContextItem> contexts = contextRetrievalService.retrieveEnhancedContexts(
                originalQuestion,
                rewrittenQuestion,
                request.safeTopK(),
                request.categoryId()
        );

        List<RagCitation> citations = contextRetrievalService.buildCitations(contexts);
        sseEmitterSender.sendContexts(emitter, citations);

        if (contexts.isEmpty()) {
            sseEmitterSender.sendRejectEvents(emitter, NO_CONTEXT_ANSWER, REJECT_REASON_NO_CONTEXT);
            return emitter;
        }

        if (relevanceEvaluator.isLowRelevance(contexts)) {
            sseEmitterSender.sendRejectEvents(emitter, WEAK_CONTEXT_ANSWER, REJECT_REASON_LOW_RELEVANCE);
            return emitter;
        }

        String prompt = promptBuilder.buildEnhancedAnswerPrompt(
                originalQuestion,
                rewrittenQuestion,
                contexts
        );

        try {
            streamingChatModel.chat(prompt, new StreamingChatResponseHandler() {

                @Override
                public void onPartialResponse(String partialResponse) {
                    sseEmitterSender.sendDelta(emitter, partialResponse);
                }

                @Override
                public void onCompleteResponse(ChatResponse completeResponse) {
                    sseEmitterSender.sendDone(emitter);
                    emitter.complete();
                }

                @Override
                public void onError(Throwable error) {
                    sseEmitterSender.sendError(emitter, error.getMessage());
                    emitter.completeWithError(error);
                }
            });
        } catch (Exception ex) {
            sseEmitterSender.sendError(emitter, ex.getMessage());
            emitter.completeWithError(ex);
        }

        return emitter;
    }

    /**
     * 校验请求参数。
     */
    private void validateRequest(RagChatRequest request) {
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, "RAG问答请求不能为空");
        BizAssert.hasText(request.question(), ErrorCode.PARAM_INVALID, "问题不能为空");
    }
}