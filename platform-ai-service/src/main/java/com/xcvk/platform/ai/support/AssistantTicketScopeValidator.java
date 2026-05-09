package com.xcvk.platform.ai.support;

import com.xcvk.platform.ai.model.internal.AssistantIntentDecision;
import com.xcvk.platform.ai.model.internal.TicketScopeValidation;
import org.springframework.stereotype.Component;

import static com.xcvk.platform.ai.constant.AssistantConstants.*;
import static com.xcvk.platform.ai.support.AssistantTextUtils.containsAny;
import static com.xcvk.platform.ai.support.AssistantTextUtils.safeText;

/**
 * 工单业务范围校验器。
 *
 * <p>这是 Agent Tool 调用前的后端安全闸门：
 * 即使大模型识别为 TICKET_CREATE，也不能直接调用 workflow-service。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-09
 */
@Component
public class AssistantTicketScopeValidator {

    /**
     * 校验请求是否属于企业服务台可办理范围。
     *
     * @param question 用户问题
     * @param decision 意图识别结果
     * @return 校验结果
     */
    public TicketScopeValidation validate(String question, AssistantIntentDecision decision) {
        String ticketTypeCode = safeText(decision.ticketTypeCode(), DEFAULT_TICKET_TYPE);

        if (!SUPPORTED_TICKET_TYPE_CODES.contains(ticketTypeCode)) {
            return TicketScopeValidation.reject(UNSUPPORTED_TICKET_ANSWER);
        }

        boolean hasBusinessKeyword = containsAny(question, BUSINESS_TICKET_KEYWORDS);
        boolean hasOutOfScopePattern = containsAny(question, OUT_OF_SCOPE_TICKET_PATTERNS);

        /*
         * 明确属于企业服务台范围的请求优先通过。
         *
         * 例如：“我想申请资产去买火星牌的显示器”
         * 虽然包含“火星”，但同时包含“资产 / 买 / 显示器”，应认为是办公资产申请。
         */
        if (hasBusinessKeyword) {
            return TicketScopeValidation.pass();
        }

        if (hasOutOfScopePattern) {
            return TicketScopeValidation.reject(UNSUPPORTED_TICKET_ANSWER);
        }

        return TicketScopeValidation.reject(UNSUPPORTED_TICKET_ANSWER);
    }
}