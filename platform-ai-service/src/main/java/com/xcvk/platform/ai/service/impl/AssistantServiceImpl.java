package com.xcvk.platform.ai.service.impl;

import com.xcvk.platform.ai.assembler.AssistantLogAssembler;
import com.xcvk.platform.ai.assembler.AssistantTicketAssembler;
import com.xcvk.platform.ai.model.dto.AssistantChatRequest;
import com.xcvk.platform.ai.model.dto.RagChatRequest;
import com.xcvk.platform.ai.model.entity.AiAgentExecutionLog;
import com.xcvk.platform.ai.model.internal.AssistantIntentDecision;
import com.xcvk.platform.ai.model.internal.TicketScopeValidation;
import com.xcvk.platform.ai.model.vo.AssistantChatResponse;
import com.xcvk.platform.ai.model.vo.AssistantTicketVO;
import com.xcvk.platform.ai.model.vo.RagChatResponse;
import com.xcvk.platform.ai.service.AgentExecutionLogService;
import com.xcvk.platform.ai.service.AssistantService;
import com.xcvk.platform.ai.service.RagChatService;
import com.xcvk.platform.ai.support.AssistantIntentClassifier;
import com.xcvk.platform.ai.support.AssistantTicketScopeValidator;
import com.xcvk.platform.ai.tool.CreateTicketTool;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.xcvk.platform.ai.constant.AssistantConstants.INTENT_TICKET_CREATE;

/**
 * 智能助手服务实现。
 *
 * <p>当前类只负责 Assistant 主流程编排：
 * 登录态校验、意图识别分发、RAG 调用、Tool 调用和日志保存。</p>
 *
 * @author Programmer
 * @version 1.1
 * @date 2026-05-09
 */
@Service
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    private final RagChatService ragChatService;

    private final AssistantIntentClassifier intentClassifier;

    private final AssistantTicketScopeValidator ticketScopeValidator;

    private final CreateTicketTool createTicketTool;

    private final AssistantLogAssembler logAssembler;

    private final AssistantTicketAssembler ticketAssembler;

    private final AgentExecutionLogService agentExecutionLogService;

    @Override
    public AssistantChatResponse chat(CurrentLoginIdentity identity, AssistantChatRequest request) {
        validateCurrentLoginIdentity(identity);
        validateRequest(request);

        AiAgentExecutionLog executionLog = logAssembler.buildBaseExecutionLog(identity, request);

        try {
            AssistantIntentDecision decision = intentClassifier.classify(request.question());
            logAssembler.fillIntentLog(executionLog, decision);

            AssistantChatResponse response = dispatch(identity, request, decision, executionLog);

            logAssembler.fillResponseLog(executionLog, response);
            return response;
        } catch (Exception ex) {
            logAssembler.markFailed(executionLog, ex);
            throw ex;
        } finally {
            agentExecutionLogService.saveSafely(executionLog);
        }
    }

    /**
     * 根据意图分发处理。
     */
    private AssistantChatResponse dispatch(CurrentLoginIdentity identity,
                                           AssistantChatRequest request,
                                           AssistantIntentDecision decision,
                                           AiAgentExecutionLog executionLog) {
        if (INTENT_TICKET_CREATE.equals(decision.intent())) {
            return handleTicketCreate(identity, request, decision, executionLog);
        }

        return handleKnowledgeQa(request, executionLog);
    }

    /**
     * 处理知识问答。
     */
    private AssistantChatResponse handleKnowledgeQa(AssistantChatRequest request,
                                                    AiAgentExecutionLog executionLog) {
        RagChatResponse ragResponse = ragChatService.chat(new RagChatRequest(
                request.question(),
                request.safeTopK(),
                request.categoryId()
        ));

        logAssembler.markKnowledgeQaSuccess(executionLog);

        return AssistantChatResponse.rag(ragResponse.answer(), ragResponse);
    }

    /**
     * 处理创建工单。
     */
    private AssistantChatResponse handleTicketCreate(CurrentLoginIdentity identity,
                                                     AssistantChatRequest request,
                                                     AssistantIntentDecision decision,
                                                     AiAgentExecutionLog executionLog) {
        TicketScopeValidation validation = ticketScopeValidator.validate(request.question(), decision);

        if (!validation.passed()) {
            logAssembler.markUnsupported(executionLog, validation.reason());
            return AssistantChatResponse.unsupported(validation.reason());
        }

        if (!request.autoCreateTicketOrTrue()) {
            logAssembler.markPendingConfirm(executionLog);
            return AssistantChatResponse.ticketPendingConfirm(
                    ticketAssembler.buildPendingConfirmAnswer(decision)
            );
        }

        AssistantTicketVO ticket = createTicketTool.create(identity, request, decision, executionLog);

        return AssistantChatResponse.ticketCreated(
                ticketAssembler.buildTicketCreatedAnswer(ticket),
                ticket
        );
    }

    /**
     * 校验当前登录身份。
     */
    private void validateCurrentLoginIdentity(CurrentLoginIdentity identity) {
        BizAssert.notNull(identity, ErrorCode.PARAM_INVALID, "当前登录身份不能为空");
        BizAssert.notNull(identity.userId(), ErrorCode.PARAM_INVALID, "当前登录用户ID不能为空");
    }

    /**
     * 校验请求。
     */
    private void validateRequest(AssistantChatRequest request) {
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, "智能助手请求不能为空");
        BizAssert.hasText(request.question(), ErrorCode.PARAM_INVALID, "问题不能为空");
    }
}