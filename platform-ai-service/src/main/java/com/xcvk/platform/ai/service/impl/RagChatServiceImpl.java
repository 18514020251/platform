package com.xcvk.platform.ai.service.impl;

import com.xcvk.platform.ai.constant.AssistantPromptTemplates;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 问答服务实现类
 *
 * @author Programmer
 * @version 1.1
 * @date 2026-05-08
 */
@Service
@Slf4j
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
     * 双路召回阶段每一路拉取的候选数量。
     *
     * <p>原问题和改写问题各取 Top10，然后在 AI 服务内做二次融合排序。</p>
     */
    private static final int RETRIEVE_STAGE_TOPK = 10;

    /**
     * 双路 RRF 融合参数。
     *
     * <p>标准 RRF 公式：1 / (k + rank)，rank 从 1 开始。</p>
     */
    private static final int QUERY_RRF_K = 60;

    /**
     * 低相关判断只看最终排序靠前的片段，而不是依赖固定分数阈值。
     */
    private static final int RELEVANCE_TOP_WINDOW = 3;

    private static final String MATCH_TYPE_TEXT = "TEXT";

    private static final String MATCH_TYPE_BOTH = "BOTH";

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

    private static final String PROMPT_REWRITTEN_QUESTION_TITLE = "【检索增强问题】\n";

    private static final String PROMPT_REQUIREMENTS_TITLE = "【回答要求】\n";

    private static final String PROMPT_REQUIREMENTS_CONTENT = """
            1. 只基于知识库内容回答，不要编造。
            2. 如果知识库片段中已经出现相关章节、条款或明确描述，不要声称知识库未明确说明。
            3. 回答要简洁、清晰，优先使用条目化列表。
            4. 回答末尾请标注依据的片段编号，例如：依据：片段2、片段3。
            5. 如果知识库内容确实不足，请只说明“当前知识库未提供足够信息”，不要扩展推测。
            6. 如果多个片段都与问题相关，请综合所有相关片段，不要只回答其中一个片段中的部分条目。
            """;

    private static final String PROMPT_REWRITE_REQUIREMENTS_CONTENT = """
            7. 检索增强问题只能用于辅助理解知识库片段，不能作为新的用户问题，也不能基于它引入额外假设。
            8. 如果用户原始问题和检索增强问题存在差异，请优先遵循用户原始问题。
            """;

    private final Object emitterLock = new Object();

    private final KnowledgeRagContextClient knowledgeRagContextClient;

    private final ChatModel chatModel;

    private final StreamingChatModel streamingChatModel;

    @Override
    public RagChatResponse chat(RagChatRequest request) {
        validateRequest(request);

        String originalQuestion = request.question();
        String rewrittenQuestion = rewriteQuestion(originalQuestion);

        logQueryRewrite(originalQuestion, rewrittenQuestion);

        List<KnowledgeRagContextItem> contexts = retrieveEnhancedContexts(
                originalQuestion,
                rewrittenQuestion,
                request.safeTopK(),
                request.categoryId()
        );

        List<RagCitation> citations = buildCitations(contexts);

        if (CollectionUtils.isEmpty(contexts)) {
            return RagChatResponse.rejected(NO_CONTEXT_ANSWER, REJECT_REASON_NO_CONTEXT, citations);
        }

        if (isLowRelevance(contexts)) {
            return RagChatResponse.rejected(WEAK_CONTEXT_ANSWER, REJECT_REASON_LOW_RELEVANCE, citations);
        }

        String prompt = buildPostProcessingPrompts(originalQuestion, rewrittenQuestion, contexts);

        String answer = chatModel.chat(prompt);

        return RagChatResponse.answered(answer, citations);
    }

    @Override
    public SseEmitter chatStream(RagChatRequest request) {
        validateRequest(request);

        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

        String originalQuestion = request.question();
        String rewrittenQuestion = rewriteQuestion(originalQuestion);

        logQueryRewrite(originalQuestion, rewrittenQuestion);

        List<KnowledgeRagContextItem> contexts = retrieveEnhancedContexts(
                originalQuestion,
                rewrittenQuestion,
                request.safeTopK(),
                request.categoryId()
        );

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

        String prompt = buildPostProcessingPrompts(originalQuestion, rewrittenQuestion, contexts);

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
     * 双路增强召回。
     *
     * <p>原问题和改写问题各自召回一批候选片段，然后基于 chunkId 去重，
     * 使用查询层 RRF 做二次融合排序。</p>
     */
    private List<KnowledgeRagContextItem> retrieveEnhancedContexts(String originalQuestion,
                                                                   String rewrittenQuestion,
                                                                   int finalTopK,
                                                                   Long categoryId) {
        List<KnowledgeRagContextItem> originalContexts = retrieveContextsByQuestion(
                originalQuestion,
                RETRIEVE_STAGE_TOPK,
                categoryId
        );

        if (!StringUtils.hasText(rewrittenQuestion)
                || safeText(originalQuestion).equals(safeText(rewrittenQuestion))) {
            return mergeAndRerank(originalContexts, List.of(), finalTopK);
        }

        List<KnowledgeRagContextItem> rewrittenContexts = retrieveContextsByQuestion(
                rewrittenQuestion,
                RETRIEVE_STAGE_TOPK,
                categoryId
        );

        return mergeAndRerank(originalContexts, rewrittenContexts, finalTopK);
    }

    /**
     * 根据问题检索知识片段。
     */
    private List<KnowledgeRagContextItem> retrieveContextsByQuestion(String question,
                                                                     Integer topK,
                                                                     Long categoryId) {
        if (!StringUtils.hasText(question)) {
            return List.of();
        }

        KnowledgeRagContextRequest contextRequest = new KnowledgeRagContextRequest(
                question,
                topK,
                categoryId
        );

        try {
            List<KnowledgeRagContextItem> contexts =
                    knowledgeRagContextClient.retrieveContexts(contextRequest);

            return contexts == null ? List.of() : contexts;
        } catch (Exception e) {
            log.warn("RAG知识片段检索失败，question={}, categoryId={}", question, categoryId, e);
            return List.of();
        }
    }

    /**
     * 合并并重排双路召回结果。
     *
     * <p>注意：这里的排序分数只使用查询层 RRF。</p>
     *
     * <p>知识库服务返回的 item.finalScore() 只作为同分时的辅助排序依据，
     * 不再和 RRF 分数相加，避免语义混乱。</p>
     */
    private List<KnowledgeRagContextItem> mergeAndRerank(List<KnowledgeRagContextItem> originalContexts,
                                                         List<KnowledgeRagContextItem> rewrittenContexts,
                                                         int finalTopK) {
        if (CollectionUtils.isEmpty(originalContexts) && CollectionUtils.isEmpty(rewrittenContexts)) {
            return List.of();
        }

        Map<Long, RerankCandidate> candidateMap = new LinkedHashMap<>();

        addToCandidateMap(candidateMap, originalContexts, true);
        addToCandidateMap(candidateMap, rewrittenContexts, false);

        return candidateMap.values()
                .stream()
                .sorted((left, right) -> {
                    int rrfCompare = Float.compare(right.finalRrfScore(), left.finalRrfScore());
                    if (rrfCompare != 0) {
                        return rrfCompare;
                    }

                    int sourceScoreCompare = Float.compare(right.bestSourceScore, left.bestSourceScore);
                    if (sourceScoreCompare != 0) {
                        return sourceScoreCompare;
                    }

                    int rankCompare = Integer.compare(left.bestRank, right.bestRank);
                    if (rankCompare != 0) {
                        return rankCompare;
                    }

                    return Long.compare(
                            safeChunkId(left.bestItem),
                            safeChunkId(right.bestItem)
                    );
                })
                .limit(finalTopK)
                .map(RerankCandidate::toContextItem)
                .toList();
    }

    /**
     * 将单路召回结果加入候选集合。
     */
    private void addToCandidateMap(Map<Long, RerankCandidate> candidateMap,
                                   List<KnowledgeRagContextItem> contexts,
                                   boolean fromOriginalQuestion) {
        if (CollectionUtils.isEmpty(contexts)) {
            return;
        }

        for (int index = 0; index < contexts.size(); index++) {
            KnowledgeRagContextItem context = contexts.get(index);

            if (context == null || context.chunkId() == null) {
                continue;
            }

            int rank = index + 1;

            RerankCandidate candidate = candidateMap.computeIfAbsent(
                    context.chunkId(),
                    ignored -> new RerankCandidate()
            );

            candidate.add(context, rank, fromOriginalQuestion);
        }
    }

    /**
     * 查询层 RRF 分数。
     *
     * @param rank 召回列表中的排名，从 1 开始
     */
    private float queryRrfScore(int rank) {
        return 1.0F / (QUERY_RRF_K + rank);
    }

    /**
     * 合并 TEXT / VECTOR / BOTH 命中类型。
     */
    private String mergeMatchType(String currentMatchType, String newMatchType) {
        if (!StringUtils.hasText(currentMatchType)) {
            return newMatchType;
        }

        if (!StringUtils.hasText(newMatchType)) {
            return currentMatchType;
        }

        if (MATCH_TYPE_BOTH.equalsIgnoreCase(currentMatchType)
                || MATCH_TYPE_BOTH.equalsIgnoreCase(newMatchType)) {
            return MATCH_TYPE_BOTH;
        }

        if (!currentMatchType.equalsIgnoreCase(newMatchType)) {
            return MATCH_TYPE_BOTH;
        }

        return currentMatchType;
    }

    /**
     * RRF 候选片段。
     *
     * <p>这里明确区分两类分数：</p>
     * <ul>
     *     <li>originalQueryRrfScore / rewrittenQueryRrfScore：AI 服务层的双路查询 RRF 分数。</li>
     *     <li>bestSourceScore：知识库服务返回的原始融合分数，只用于同分排序。</li>
     * </ul>
     */
    private class RerankCandidate {

        private KnowledgeRagContextItem bestItem;

        /**
         * 原问题召回中的 RRF 分数。
         */
        private float originalQueryRrfScore;

        /**
         * 改写问题召回中的 RRF 分数。
         */
        private float rewrittenQueryRrfScore;

        /**
         * 知识库服务返回的最高原始分数。
         *
         * <p>只作为排序兜底，不参与最终 RRF 分数计算。</p>
         */
        private float bestSourceScore;

        /**
         * 该 chunk 在两路召回中出现过的最好排名。
         */
        private int bestRank = Integer.MAX_VALUE;

        /**
         * TEXT / VECTOR / BOTH。
         */
        private String matchType;

        private void add(KnowledgeRagContextItem item, int rank, boolean fromOriginalQuestion) {
            float currentRrfScore = queryRrfScore(rank);

            if (fromOriginalQuestion) {
                this.originalQueryRrfScore = Math.max(this.originalQueryRrfScore, currentRrfScore);
            } else {
                this.rewrittenQueryRrfScore = Math.max(this.rewrittenQueryRrfScore, currentRrfScore);
            }

            this.bestRank = Math.min(this.bestRank, rank);
            this.matchType = mergeMatchType(this.matchType, item.matchType());

            float sourceScore = safeScore(item.finalScore());

            if (this.bestItem == null || sourceScore > this.bestSourceScore) {
                this.bestItem = item;
                this.bestSourceScore = sourceScore;
            }
        }

        private float finalRrfScore() {
            return originalQueryRrfScore + rewrittenQueryRrfScore;
        }

        private KnowledgeRagContextItem toContextItem() {
            return new KnowledgeRagContextItem(
                    bestItem.chunkId(),
                    bestItem.documentId(),
                    bestItem.chunkNo(),
                    bestItem.documentTitle(),
                    bestItem.categoryName(),
                    bestItem.content(),
                    finalRrfScore(),
                    matchType
            );
        }
    }

    /**
     * 重写问题。
     *
     * <p>问题改写失败时，降级使用原问题，不能影响主链路。</p>
     */
    private String rewriteQuestion(String originalQuestion) {
        String prompt = buildQuestionRewritePrompt(originalQuestion);

        try {
            String rewrittenQuestion = chatModel.chat(prompt);
            rewrittenQuestion = cleanRewrittenQuestion(rewrittenQuestion);

            if (!StringUtils.hasText(rewrittenQuestion)) {
                return originalQuestion;
            }

            if (rewrittenQuestion.length() > 300) {
                log.warn("问题改写结果过长，使用原问题。originalQuestion={}, rewrittenQuestion={}",
                        originalQuestion, rewrittenQuestion);
                return originalQuestion;
            }

            log.info("问题改写完成，originalQuestion={}, rewrittenQuestion={}",
                    originalQuestion, rewrittenQuestion);

            return rewrittenQuestion;
        } catch (Exception e) {
            log.warn("问题改写失败，使用原问题。originalQuestion={}", originalQuestion, e);
            return originalQuestion;
        }
    }

    /**
     * 构建问题改写 Prompt。
     */
    private String buildQuestionRewritePrompt(String originalQuestion) {
        return AssistantPromptTemplates.QUESTION_REWRITE_PROMPT_TEMPLATE
                .replace("{question}", originalQuestion);
    }

    /**
     * 清洗问题改写结果。
     */
    private String cleanRewrittenQuestion(String rewrittenQuestion) {
        if (!StringUtils.hasText(rewrittenQuestion)) {
            return rewrittenQuestion;
        }

        return rewrittenQuestion
                .replace("```text", "")
                .replace("```", "")
                .replace("改写后的检索查询文本：", "")
                .replace("改写后的问题：", "")
                .replace("检索查询文本：", "")
                .replace("\"", "")
                .replace("“", "")
                .replace("”", "")
                .trim();
    }

    /**
     * 日志记录问题重写信息。
     */
    private void logQueryRewrite(String originalQuestion, String rewrittenQuestion) {
        boolean rewriteApplied = StringUtils.hasText(rewrittenQuestion)
                && !safeText(originalQuestion).equals(safeText(rewrittenQuestion));

        log.info("RAG问题重写完成，rewriteApplied={}, originalQuestion={}, rewrittenQuestion={}",
                rewriteApplied,
                originalQuestion,
                rewrittenQuestion
        );
    }

    /**
     * 构建增强 RAG 最终回答 Prompt。
     *
     * <p>originalQuestion 是用户真实问题，最终回答必须以它为准。</p>
     * <p>rewrittenQuestion 是检索增强问题，只用于辅助模型理解召回上下文。</p>
     */
    private String buildPostProcessingPrompts(String originalQuestion,
                                              String rewrittenQuestion,
                                              List<KnowledgeRagContextItem> contexts) {
        return buildAnswerPrompt(originalQuestion, rewrittenQuestion, contexts, true);
    }

    /**
     * 兼容旧调用的 Prompt 构建方法。
     */
    private String buildPrompt(String question, List<KnowledgeRagContextItem> contexts) {
        return buildAnswerPrompt(question, null, contexts, false);
    }

    /**
     * 构建最终回答 Prompt。
     */
    private String buildAnswerPrompt(String question,
                                     String rewrittenQuestion,
                                     List<KnowledgeRagContextItem> contexts,
                                     boolean includeRewriteInfo) {
        StringBuilder builder = new StringBuilder();

        builder.append(PROMPT_SYSTEM_ROLE).append("\n");

        builder.append(PROMPT_KNOWLEDGE_BASE_TITLE);
        appendKnowledgeContexts(builder, contexts);

        builder.append(PROMPT_QUESTION_TITLE);
        builder.append(safeText(question)).append("\n\n");

        if (includeRewriteInfo
                && StringUtils.hasText(rewrittenQuestion)
                && !safeText(question).equals(safeText(rewrittenQuestion))) {
            builder.append(PROMPT_REWRITTEN_QUESTION_TITLE);
            builder.append(safeText(rewrittenQuestion)).append("\n");
            builder.append("说明：检索增强问题是系统为了召回知识库内容生成的辅助表达，")
                    .append("最终回答必须以用户原始问题为准，不要偏离用户原始意图。")
                    .append("\n\n");
        }

        builder.append(PROMPT_REQUIREMENTS_TITLE);
        builder.append(PROMPT_REQUIREMENTS_CONTENT);

        if (includeRewriteInfo
                && StringUtils.hasText(rewrittenQuestion)
                && !safeText(question).equals(safeText(rewrittenQuestion))) {
            builder.append(PROMPT_REWRITE_REQUIREMENTS_CONTENT);
        }

        return builder.toString();
    }

    /**
     * 拼接知识库片段。
     */
    private void appendKnowledgeContexts(StringBuilder builder, List<KnowledgeRagContextItem> contexts) {
        if (CollectionUtils.isEmpty(contexts)) {
            return;
        }

        for (int index = 0; index < contexts.size(); index++) {
            KnowledgeRagContextItem context = contexts.get(index);
            builder.append(PROMPT_SECTION_TEMPLATE
                    .replace("{index}", String.valueOf(index + 1))
                    .replace("{documentTitle}", safeText(context.documentTitle()))
                    .replace("{categoryName}", safeText(context.categoryName()))
                    .replace("{content}", safeText(context.content())));
        }
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
     *
     * <p>优化点：不再使用固定分数阈值判断相关性。</p>
     *
     * <p>原因：当前 finalScore 在 AI 服务层已经变成“原问题 / 改写问题的查询层 RRF 分数”，
     * 不是知识库服务的原始相关性分数，也不适合使用 0.04、0.015 这类绝对阈值。</p>
     *
     * <p>当前策略：</p>
     * <ul>
     *     <li>最终 TopN 中存在 BOTH，认为有较可靠证据。</li>
     *     <li>最终 TopN 中存在 TEXT，认为至少关键词层面有证据。</li>
     *     <li>如果最终靠前片段全部是 VECTOR，先保守拒答，避免语义误召回导致乱答。</li>
     * </ul>
     */
    private boolean isLowRelevance(List<KnowledgeRagContextItem> contexts) {
        if (CollectionUtils.isEmpty(contexts)) {
            return true;
        }

        int topWindow = Math.min(RELEVANCE_TOP_WINDOW, contexts.size());

        boolean hasHybridEvidenceInTopWindow = contexts.stream()
                .limit(topWindow)
                .anyMatch(context -> MATCH_TYPE_BOTH.equalsIgnoreCase(context.matchType()));

        if (hasHybridEvidenceInTopWindow) {
            return false;
        }

        boolean hasTextEvidenceInTopWindow = contexts.stream()
                .limit(topWindow)
                .anyMatch(context -> MATCH_TYPE_TEXT.equalsIgnoreCase(context.matchType()));

        return !hasTextEvidenceInTopWindow;
    }

    /**
     * 校验请求参数。
     */
    private void validateRequest(RagChatRequest request) {
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, "RAG问答请求不能为空");
        BizAssert.hasText(request.question(), ErrorCode.PARAM_INVALID, "问题不能为空");
    }

    /**
     * 安全获取检索分数。
     */
    private float safeScore(Float score) {
        return score == null ? 0.0F : score;
    }

    /**
     * 安全获取 chunkId。
     */
    private long safeChunkId(KnowledgeRagContextItem item) {
        if (item == null || item.chunkId() == null) {
            return Long.MAX_VALUE;
        }
        return item.chunkId();
    }

    /**
     * 安全获取文本，null 转空字符串并去除首尾空格。
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
     * 发送 SSE 事件。
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