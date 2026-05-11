package com.xcvk.platform.ai.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcvk.platform.ai.constant.AssistantConstants;
import com.xcvk.platform.ai.constant.AssistantPromptTemplates;
import com.xcvk.platform.ai.model.internal.AssistantIntentDecision;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;

import static com.xcvk.platform.ai.constant.AssistantConstants.*;
import static com.xcvk.platform.ai.support.AssistantTextUtils.*;

/**
 * Assistant 意图识别器。
 *
 * <p>负责 LLM 意图识别、JSON 抽取、兜底规则和意图结果规范化。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-09
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AssistantIntentClassifier {

    private final ChatModel chatModel;

    private final ObjectMapper objectMapper;

    /**
     * 识别用户意图。
     *
     * @param question 用户问题
     * @return 意图识别结果
     */
    public AssistantIntentDecision classify(String question) {
        try {
            String prompt = AssistantPromptTemplates.INTENT_PROMPT_TEMPLATE.replace("{question}", question);

            String modelOutput = chatModel.chat(prompt);

            String json = extractJson(modelOutput);

            AssistantIntentDecision decision = objectMapper.readValue(json, AssistantIntentDecision.class);
            return normalizeDecision(question, decision);
        } catch (Exception ex) {
            log.warn("LLM意图识别失败，降级为关键词规则，question={}", question, ex);
            return fallbackClassify(question);
        }
    }

    private AssistantIntentDecision fallbackClassify(String question) {
        String lower = question.toLowerCase(Locale.ROOT);

        boolean explicitTicket = containsAny(
                lower,
                "工单", "提单", "报修", "帮我处理", "帮我开通", "创建", "申请"
        );

        boolean issue = containsAny(
                lower,
                "不能", "无法", "连不上", "登录不上", "报错", "故障", "坏了", "蓝屏", "没权限", "访问不了"
        );

        if (explicitTicket || issue) {
            String ticketTypeCode = inferTicketType(question);
            return new AssistantIntentDecision(
                    INTENT_TICKET_CREATE,
                    0.65,
                    ticketTypeCode,
                    buildDefaultTitle(question),
                    question,
                    inferPriority(question)
            );
        }

        return new AssistantIntentDecision(
                INTENT_KNOWLEDGE_QA,
                0.60,
                "",
                "",
                "",
                DEFAULT_PRIORITY
        );
    }

    private AssistantIntentDecision normalizeDecision(String question, AssistantIntentDecision decision) {
        if (decision == null || !StringUtils.hasText(decision.intent())) {
            return fallbackClassify(question);
        }

        String intent = decision.intent().trim();

        if (!INTENT_TICKET_CREATE.equals(intent)) {
            return new AssistantIntentDecision(
                    INTENT_KNOWLEDGE_QA,
                    decision.confidence(),
                    "",
                    "",
                    "",
                    DEFAULT_PRIORITY
            );
        }

        String ticketTypeCode = StringUtils.hasText(decision.ticketTypeCode())
                ? decision.ticketTypeCode().trim()
                : inferTicketType(question);

        String title = StringUtils.hasText(decision.title())
                ? decision.title().trim()
                : buildDefaultTitle(question);

        String content = StringUtils.hasText(decision.content())
                ? decision.content().trim()
                : question;

        String priority = StringUtils.hasText(decision.priority())
                ? decision.priority().trim()
                : inferPriority(question);

        return new AssistantIntentDecision(
                INTENT_TICKET_CREATE,
                decision.confidence(),
                ticketTypeCode,
                title,
                content,
                priority
        );
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

    private String inferTicketType(String question) {
        String lower = question.toLowerCase(Locale.ROOT);

        if (containsAny(lower, "vpn", "远程办公")) {
            return "VPN_APPLY";
        }

        if (containsAny(lower, "账号", "密码", "登录", "oa", "git", "权限", "无权限")) {
            return "ACCOUNT_ISSUE";
        }

        if (containsAny(lower, "测试环境", "生产环境", "发布权限", "仓库权限", "环境权限")) {
            return "ENV_PERMISSION";
        }

        if (containsAny(lower, "资产", "采购", "购买", "买", "领用", "显示器", "办公设备", "办公用品")) {
            return AssistantConstants.DEFAULT_TICKET_TYPE;
        }

        if (containsAny(lower, "电脑", "打印机", "网络", "蓝屏", "设备", "鼠标", "键盘")) {
            return AssistantConstants.DEFAULT_TICKET_TYPE;
        }

        return AssistantConstants.DEFAULT_TICKET_TYPE;
    }

    private String inferPriority(String question) {
        String lower = question.toLowerCase(Locale.ROOT);

        if (containsAny(lower, "生产", "紧急", "严重", "无法办公", "线上")) {
            return "HIGH";
        }

        if (containsAny(lower, "咨询", "了解", "低优先级")) {
            return "LOW";
        }

        return DEFAULT_PRIORITY;
    }
}