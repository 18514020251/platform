package com.xcvk.platform.ai.service.rag;

import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextItem;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * RAG 召回相关性判断器。
 *
 * <p>
 * 注意：
 * 这里判断的不是“有没有命中切片”，而是“命中的切片是否足以支撑回答”。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagRelevanceEvaluator {

    private static final int RELEVANCE_TOP_WINDOW = 3;

    private static final String MATCH_TYPE_TEXT = "TEXT";

    private static final String MATCH_TYPE_BOTH = "BOTH";

    private static final int MAX_CONTEXT_LENGTH = 600;

    private final ChatModel chatModel;

    /**
     * 判断召回结果是否低相关。
     *
     * <p>
     * 这个方法保留兼容用。
     * 如果没有传 question，只做原来的粗判断。
     * </p>
     */
    public boolean isLowRelevance(List<KnowledgeRagContextItem> contexts) {
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
     * 判断召回结果是否低相关。
     *
     * <p>
     * MVP 增强版：
     * 1. 先做原来的检索层粗判断。
     * 2. 如果粗判断已经低相关，直接返回 true。
     * 3. 如果粗判断认为不低相关，再调用 LLM 判断这些切片是否真的能回答用户问题。
     * </p>
     */
    public boolean isLowRelevance(String question, List<KnowledgeRagContextItem> contexts) {
        if (CollectionUtils.isEmpty(contexts)) {
            return true;
        }

        boolean coarseLowRelevance = isLowRelevance(contexts);

        if (coarseLowRelevance) {
            return true;
        }

        /*
         * 走到这里说明：
         * 检索层认为 Top3 中有 TEXT / BOTH 命中。
         *
         * 但这只能说明“关键词或语义相关”，不能说明“足以回答”。
         * 所以这里追加一个 Answerability Check。
         */
        return !isAnswerableByContexts(question, contexts);
    }

    /**
     * 判断当前上下文是否足以回答用户问题。
     */
    private boolean isAnswerableByContexts(String question, List<KnowledgeRagContextItem> contexts) {
        try {
            String prompt = buildAnswerabilityPrompt(question, contexts);

            String output = chatModel.chat(prompt);

            String normalized = output == null ? "" : output.trim().toUpperCase();

            if (normalized.contains("NOT_ANSWERABLE")) {
                return false;
            }

            if (normalized.contains("ANSWERABLE")) {
                return true;
            }

            log.warn("Answerability Check 输出格式异常，output={}", output);

            return true;
        } catch (Exception ex) {
            /*
             * 这里不要因为判断器异常影响正常 RAG。
             * 判断失败时保守认为可以回答。
             */
            log.warn("Answerability Check 执行失败，保守放行", ex);
            return true;
        }
    }

    private String buildAnswerabilityPrompt(String question, List<KnowledgeRagContextItem> contexts) {
        return """
                你是企业知识库 RAG 的证据充分性判断器。

                你的任务不是回答用户问题，而是判断：
                给定的知识库片段是否足以支撑回答用户问题。

                请严格遵守：
                1. 只能输出 ANSWERABLE 或 NOT_ANSWERABLE。
                2. 不要输出解释。
                3. 不要根据常识、经验或猜测判断。
                4. 只有当知识库片段中明确包含回答该问题所需的流程、规则、入口、处理人、处理方式或结论时，才输出 ANSWERABLE。
                5. 如果知识库片段只是命中了相似关键词，但不能直接支撑回答，输出 NOT_ANSWERABLE。
                6. 如果问题问的是具体内部流程，但片段只包含泛泛说明，输出 NOT_ANSWERABLE。
                7. 如果片段内容和问题属于同一大类，但没有覆盖用户问的具体事项，输出 NOT_ANSWERABLE。

                用户问题：
                %s

                知识库片段：
                %s

                请输出：
                """.formatted(
                question,
                buildContextText(contexts)
        );
    }

    private String buildContextText(List<KnowledgeRagContextItem> contexts) {
        if (CollectionUtils.isEmpty(contexts)) {
            return "";
        }

        StringBuilder builder = new StringBuilder();

        int topWindow = Math.min(RELEVANCE_TOP_WINDOW, contexts.size());

        for (int i = 0; i < topWindow; i++) {
            KnowledgeRagContextItem context = contexts.get(i);

            if (context == null) {
                continue;
            }

            builder.append("【片段")
                    .append(i + 1)
                    .append("】\n");

            builder.append("文档标题：")
                    .append(safeText(context.documentTitle()))
                    .append("\n");

            builder.append("分类：")
                    .append(safeText(context.categoryName()))
                    .append("\n");

            builder.append("命中类型：")
                    .append(safeText(context.matchType()))
                    .append("\n");

            builder.append("内容：")
                    .append(truncate(context.content(), MAX_CONTEXT_LENGTH))
                    .append("\n\n");
        }

        return builder.toString();
    }

    private String safeText(String text) {
        return StringUtils.hasText(text) ? text : "";
    }

    private String truncate(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength) + "...";
    }
}