package com.xcvk.platform.ai.service;

import com.xcvk.platform.ai.model.entity.AiKnowledgeGapTicket;
import com.xcvk.platform.ai.model.internal.KnowledgeGapTicketDecision;
import com.xcvk.platform.ai.model.vo.AssistantTicketVO;

import java.util.Optional;

/**
 * 知识缺口转工单去重服务。
 */
public interface KnowledgeGapTicketDedupService {

    /**
     * LLM 调用前，根据用户原话查重。
     */
    Optional<AiKnowledgeGapTicket> findByRawQuestion(String rawQuestion);

    /**
     * LLM 调用后，根据规范化问题查重。
     */
    Optional<AiKnowledgeGapTicket> findByNormalizedQuestion(KnowledgeGapTicketDecision decision);

    /**
     * 记录本次知识缺口创建的工单。
     */
    void recordCreatedTicket(Long userId,
                             String rawQuestion,
                             KnowledgeGapTicketDecision decision,
                             AssistantTicketVO ticket);

    /**
     * 增加重复命中次数。
     */
    void increaseHitCount(Long id);
}