package com.xcvk.platform.ai.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcvk.platform.ai.constant.AssistantConstants;
import com.xcvk.platform.ai.constant.AssistantPromptTemplates;
import com.xcvk.platform.ai.model.internal.AssistantIntentDecision;
import com.xcvk.platform.ai.model.internal.KnowledgeGapTicketDecision;
import com.xcvk.platform.ai.model.vo.RagChatResponse;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Set;

import static com.xcvk.platform.ai.constant.AssistantConstants.DEFAULT_PRIORITY;
import static com.xcvk.platform.ai.constant.AssistantConstants.DEFAULT_TICKET_TYPE;
import static com.xcvk.platform.ai.constant.AssistantConstants.INTENT_TICKET_CREATE;
import static com.xcvk.platform.ai.constant.AssistantConstants.PRIORITY_HIGH;
import static com.xcvk.platform.ai.constant.AssistantConstants.PRIORITY_LOW;
import static com.xcvk.platform.ai.constant.AssistantConstants.PRIORITY_MEDIUM;
import static com.xcvk.platform.ai.constant.AssistantConstants.TICKET_TYPE_ACCOUNT_ISSUE;
import static com.xcvk.platform.ai.constant.AssistantConstants.TICKET_TYPE_ENV_PERMISSION;
import static com.xcvk.platform.ai.constant.AssistantConstants.TICKET_TYPE_VPN_APPLY;
import static com.xcvk.platform.ai.support.AssistantTextUtils.buildDefaultTitle;
import static com.xcvk.platform.ai.support.AssistantTextUtils.containsAny;
import static com.xcvk.platform.ai.support.AssistantTextUtils.safeText;
import static com.xcvk.platform.ai.support.AssistantTextUtils.truncate;

/**
 * 知识缺口转工单决策器。
 *
 * <p>
 * 当 RAG 因 NO_CONTEXT 或 LOW_RELEVANCE 拒答后，
 * 本组件调用 LLM 判断用户问题是否属于企业内部知识缺口，
 * 并生成可复用 CreateTicketTool 的工单决策。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KnowledgeGapTicketDecider {

    /**
     * 只有这些 RAG 拒答原因才进入知识缺口转工单分支。
     */
    private static final Set<String> GAP_REJECT_REASONS = Set.of(
            "NO_CONTEXT",
            "LOW_RELEVANCE"
    );

    private static final double MIN_CONFIDENCE = 0.70;

    private final ChatModel chatModel;

    private final ObjectMapper objectMapper;

    /**
     * 判断当前 RAG 结果是否需要进入知识缺口处理链路。
     */
    public boolean canHandle(RagChatResponse ragResponse) {
        return ragResponse != null
                && Boolean.TRUE.equals(ragResponse.rejected())
                && GAP_REJECT_REASONS.contains(ragResponse.rejectReason());
    }

    /**
     * 调用 LLM，判断是否企业内部问题，并生成规范化工单信息。
     */
    public KnowledgeGapTicketDecision decide(String question, RagChatResponse ragResponse) {
        if (!canHandle(ragResponse)) {
            return reject("RAG 未拒答，或拒答原因不属于知识缺口");
        }

        try {
            String prompt = AssistantPromptTemplates.KNOWLEDGE_GAP_TICKET_PROMPT_TEMPLATE
                    .replace("{question}", question)
                    .replace("{rejectReason}", safeText(ragResponse.rejectReason(), "UNKNOWN"));

            String modelOutput = chatModel.chat(prompt);
            String json = extractJson(modelOutput);

            KnowledgeGapTicketDecision decision =
                    objectMapper.readValue(json, KnowledgeGapTicketDecision.class);

            return normalize(question, ragResponse, decision);
        } catch (Exception ex) {
            log.warn("知识缺口转工单 LLM 决策失败，question={}", question, ex);

            /*
             * MVP 这里选择安全兜底：
             * 如果 LLM 判断失败，不自动创建工单。
             */
            return reject("知识缺口转工单决策失败");
        }
    }

    /**
     * MVP 重复工单检测。
     *
     * <p>
     * 当前先永远返回 false。
     * 后续第二版再实现：
     * rawQuestionHash + normalizedQuestionHash + 工单状态 + Redis 锁。
     * </p>
     */
    public boolean duplicateTicketExists(String rawQuestion, KnowledgeGapTicketDecision decision) {
        // TODO：后续接入重复工单检测，避免同类问题重复创建工单。
        return false;
    }

    /**
     * 是否允许根据当前决策创建工单。
     */
    public boolean shouldCreateTicket(KnowledgeGapTicketDecision decision) {
        return decision != null
                && Boolean.TRUE.equals(decision.enterpriseRelated())
                && decision.confidence() != null
                && decision.confidence() >= MIN_CONFIDENCE
                && AssistantConstants.isSupportedTicketType(decision.ticketTypeCode());
    }

    /**
     * 转成现有 CreateTicketTool 能识别的 AssistantIntentDecision。
     */
    public AssistantIntentDecision toAssistantIntentDecision(KnowledgeGapTicketDecision decision) {
        return new AssistantIntentDecision(
                INTENT_TICKET_CREATE,
                decision.confidence(),
                decision.ticketTypeCode(),
                decision.title(),
                decision.content(),
                decision.priority()
        );
    }

    private KnowledgeGapTicketDecision normalize(String question,
                                                 RagChatResponse ragResponse,
                                                 KnowledgeGapTicketDecision decision) {
        if (decision == null || !Boolean.TRUE.equals(decision.enterpriseRelated())) {
            return reject("LLM 判断不是企业内部问题");
        }

        double confidence = decision.confidence() == null ? 0.0 : decision.confidence();

        String normalizedQuestion = safeText(decision.normalizedQuestion(), question);

        String ticketTypeCode = normalizeTicketType(
                decision.ticketTypeCode(),
                question + " " + normalizedQuestion
        );

        String title = truncate(
                safeText(decision.title(), "知识库未覆盖：" + buildDefaultTitle(normalizedQuestion)),
                30
        );

        String priority = normalizePriority(decision.priority());

        String reason = safeText(
                decision.reason(),
                "用户问题属于企业内部问题，但当前知识库未检索到可靠答案"
        );

        String content = safeText(decision.content(), "");

        if (!StringUtils.hasText(content)) {
            content = buildDefaultContent(
                    question,
                    normalizedQuestion,
                    ragResponse.rejectReason(),
                    reason
            );
        }

        return new KnowledgeGapTicketDecision(
                true,
                confidence,
                normalizedQuestion,
                ticketTypeCode,
                title,
                content,
                priority,
                reason
        );
    }

    private KnowledgeGapTicketDecision reject(String reason) {
        return new KnowledgeGapTicketDecision(
                false,
                0.0,
                "",
                "",
                "",
                "",
                DEFAULT_PRIORITY,
                reason
        );
    }

    private String buildDefaultContent(String rawQuestion,
                                       String normalizedQuestion,
                                       String rejectReason,
                                       String reason) {
        return """
                用户咨询的问题当前知识库未检索到可靠答案，请管理员补充知识库或人工处理。

                用户原始问题：
                %s

                规范化问题：
                %s

                RAG 拒答原因：
                %s

                判断原因：
                %s

                TODO：
                后续接入重复工单检测，避免同类问题重复创建工单。
                """.formatted(
                rawQuestion,
                normalizedQuestion,
                safeText(rejectReason, "UNKNOWN"),
                reason
        );
    }

    private String normalizeTicketType(String ticketTypeCode, String question) {
        if (StringUtils.hasText(ticketTypeCode)) {
            String normalized = ticketTypeCode.trim().toUpperCase(Locale.ROOT);
            if (AssistantConstants.isSupportedTicketType(normalized)) {
                return normalized;
            }
        }

        return inferTicketType(question);
    }

    private String inferTicketType(String question) {
        if (containsAny(question, "vpn", "远程办公")) {
            return TICKET_TYPE_VPN_APPLY;
        }

        if (containsAny(question, "账号", "密码", "登录", "登陆", "oa", "git", "邮箱", "权限", "无权限")) {
            return TICKET_TYPE_ACCOUNT_ISSUE;
        }

        if (containsAny(question, "测试环境", "生产环境", "发布权限", "仓库权限", "环境权限")) {
            return TICKET_TYPE_ENV_PERMISSION;
        }

        return DEFAULT_TICKET_TYPE;
    }

    private String normalizePriority(String priority) {
        if (!StringUtils.hasText(priority)) {
            return DEFAULT_PRIORITY;
        }

        String normalized = priority.trim().toUpperCase(Locale.ROOT);

        if (PRIORITY_LOW.equals(normalized)
                || PRIORITY_MEDIUM.equals(normalized)
                || PRIORITY_HIGH.equals(normalized)) {
            return normalized;
        }

        return DEFAULT_PRIORITY;
    }

    private String extractJson(String text) {
        if (!StringUtils.hasText(text)) {
            return "{}";
        }

        String cleaned = text.trim()
                .replace("```json", "")
                .replace("```", "")
                .trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }

        return cleaned;
    }
}