package com.xcvk.platform.ai.assembler;

import com.xcvk.platform.ai.model.internal.AssistantIntentDecision;
import com.xcvk.platform.ai.model.vo.AssistantTicketVO;
import org.springframework.stereotype.Component;

import static com.xcvk.platform.ai.support.AssistantTextUtils.safeText;

/**
 * Assistant 工单响应组装器。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-09
 */
@Component
public class AssistantTicketAssembler {

    /**
     * 构建工单待确认回答。
     *
     * @param decision 意图识别结果
     * @return 回答文案
     */
    public String buildPendingConfirmAnswer(AssistantIntentDecision decision) {
        return "我已识别到这是一个需要创建工单的问题，建议创建【"
                + safeText(decision.title(), "待处理问题")
                + "】工单。请确认后再提交。";
    }

    /**
     * 构建工单创建成功回答。
     *
     * @param ticket 工单结果
     * @return 回答文案
     */
    public String buildTicketCreatedAnswer(AssistantTicketVO ticket) {
        return "已为你创建工单："
                + ticket.ticketNo()
                + "，当前状态为 "
                + ticket.status()
                + "。工单类型："
                + ticket.ticketTypeCode()
                + "。";
    }
}