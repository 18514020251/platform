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

        boolean explicitQuery = containsAny(
                lower,
                "查询工单", "查看工单", "查工单", "查一下工单",
                "我的工单", "最近工单", "工单状态", "工单进度",
                "处理到哪", "处理进度", "进度怎么样",
                "待处理工单", "处理中工单", "已完成工单", "已解决工单",
                "工单详情", "工单内容"
        );

        boolean ticketNoQuery = containsTicketNo(question);

        if (explicitQuery || ticketNoQuery) {
            return new AssistantIntentDecision(
                    INTENT_TICKET_QUERY,
                    0.70,
                    "",
                    "",
                    question,
                    DEFAULT_PRIORITY
            );
        }

        boolean explicitTicketCreate = containsAny(
                lower,
                "提工单", "创建工单", "新建工单", "提交工单",
                "帮我提单", "帮我提个单", "帮我报修",
                "帮我处理", "帮我开通", "申请开通",
                "报修", "派人处理"
        );

        boolean issue = containsAny(
                lower,
                "不能", "无法", "连不上", "登录不上", "报错",
                "故障", "坏了", "蓝屏", "没权限", "访问不了"
        );

        if (explicitTicketCreate || issue) {
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

    /**
     * 判断是否有工单号
     * */
    private boolean containsTicketNo(String question) {
        if (!StringUtils.hasText(question)) {
            return false;
        }

        return question.matches(".*(?i)(TK|TICKET)[0-9A-Za-z\\-]{4,}.*");
    }


    private AssistantIntentDecision normalizeDecision(String question, AssistantIntentDecision decision) {
        if (decision == null || !StringUtils.hasText(decision.intent())) {
            return fallbackClassify(question);
        }

        String intent = decision.intent().trim();

        if (INTENT_TICKET_QUERY.equals(intent)) {
            return new AssistantIntentDecision(
                    INTENT_TICKET_QUERY,
                    decision.confidence(),
                    "",
                    "",
                    StringUtils.hasText(decision.content()) ? decision.content().trim() : question,
                    DEFAULT_PRIORITY
            );
        }

        if (INTENT_TICKET_CREATE.equals(intent)) {
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

        if (INTENT_KNOWLEDGE_QA.equals(intent)) {
            return new AssistantIntentDecision(
                    INTENT_KNOWLEDGE_QA,
                    decision.confidence(),
                    "",
                    "",
                    "",
                    DEFAULT_PRIORITY
            );
        }

        return fallbackClassify(question);
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