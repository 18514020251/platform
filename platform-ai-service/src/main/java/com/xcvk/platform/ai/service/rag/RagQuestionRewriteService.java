package com.xcvk.platform.ai.service.rag;

import com.xcvk.platform.ai.constant.AssistantPromptTemplates;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * RAG 问题改写服务。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagQuestionRewriteService {

    private final ChatModel chatModel;

    /**
     * 重写问题。
     *
     * <p>问题改写失败时降级使用原问题，不能影响主链路。</p>
     */
    public String rewriteQuestion(String originalQuestion) {
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

            logQueryRewrite(originalQuestion, rewrittenQuestion);

            return rewrittenQuestion;
        } catch (Exception e) {
            log.warn("问题改写失败，使用原问题。originalQuestion={}", originalQuestion, e);
            return originalQuestion;
        }
    }

    private String buildQuestionRewritePrompt(String originalQuestion) {
        return AssistantPromptTemplates.QUESTION_REWRITE_PROMPT_TEMPLATE
                .replace("{question}", originalQuestion);
    }

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

    private void logQueryRewrite(String originalQuestion, String rewrittenQuestion) {
        boolean rewriteApplied = StringUtils.hasText(rewrittenQuestion)
                && !safeText(originalQuestion).equals(safeText(rewrittenQuestion));

        log.info("RAG问题重写完成，rewriteApplied={}, originalQuestion={}, rewrittenQuestion={}",
                rewriteApplied,
                originalQuestion,
                rewrittenQuestion
        );
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}