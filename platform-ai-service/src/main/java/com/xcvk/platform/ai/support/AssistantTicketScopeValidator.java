package com.xcvk.platform.ai.support;

import com.xcvk.platform.ai.constant.AssistantConstants;
import com.xcvk.platform.ai.model.internal.AssistantIntentDecision;
import com.xcvk.platform.ai.model.internal.TicketScopeValidation;
import org.springframework.stereotype.Component;

import static com.xcvk.platform.ai.constant.AssistantConstants.DEFAULT_TICKET_TYPE;
import static com.xcvk.platform.ai.constant.AssistantConstants.UNSUPPORTED_TICKET_ANSWER;
import static com.xcvk.platform.ai.support.AssistantTextUtils.containsAny;
import static com.xcvk.platform.ai.support.AssistantTextUtils.safeText;

/**
 * 工单业务范围校验器。
 *
 * <p>这是 Agent Tool 调用前的后端安全闸门：
 * 即使大模型识别为 TICKET_CREATE，也不能直接调用 workflow-service。</p>
 *
 * @author Programmer
 * @version 1.1
 * @date 2026-05-09
 */
@Component
public class AssistantTicketScopeValidator {

    public TicketScopeValidation validate(String question, AssistantIntentDecision decision) {
        String ticketTypeCode = safeText(decision.ticketTypeCode(), DEFAULT_TICKET_TYPE);

        if (!AssistantConstants.isSupportedTicketType(ticketTypeCode)) {
            return TicketScopeValidation.reject(UNSUPPORTED_TICKET_ANSWER);
        }

        boolean hasBusinessKeyword = containsAny(question, AssistantConstants.businessTicketKeywords());
        boolean hasOutOfScopePattern = containsAny(question, AssistantConstants.outOfScopeTicketPatterns());

        if (hasBusinessKeyword) {
            return TicketScopeValidation.pass();
        }

        if (hasOutOfScopePattern) {
            return TicketScopeValidation.reject(UNSUPPORTED_TICKET_ANSWER);
        }

        return TicketScopeValidation.reject(UNSUPPORTED_TICKET_ANSWER);
    }
}