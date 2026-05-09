package com.xcvk.platform.ai.assembler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcvk.platform.ai.model.dto.AssistantChatRequest;
import com.xcvk.platform.ai.model.entity.AiAgentExecutionLog;
import com.xcvk.platform.ai.model.internal.AssistantIntentDecision;
import com.xcvk.platform.ai.model.vo.AssistantChatResponse;
import com.xcvk.platform.api.contract.workflow.model.CreateAiTicketRequest;
import com.xcvk.platform.api.contract.workflow.model.CreateAiTicketResponse;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.ai.support.AssistantIdentityResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.xcvk.platform.ai.constant.AssistantConstants.*;
import static com.xcvk.platform.ai.support.AssistantTextUtils.safeText;
import static com.xcvk.platform.ai.support.AssistantTextUtils.truncate;

/**
 * Assistant 执行日志组装器。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-09
 */
@Component
@RequiredArgsConstructor
public class AssistantLogAssembler {

    private final ObjectMapper objectMapper;

    private final AssistantIdentityResolver identityResolver;

    public AiAgentExecutionLog buildBaseExecutionLog(CurrentLoginIdentity identity, AssistantChatRequest request) {
        return new AiAgentExecutionLog()
                .setUserId(identity.userId())
                .setUsername(identityResolver.resolveCreatorName(identity))
                .setQuestion(request.question())
                .setToolExecuted(false)
                .setExecutionStatus(EXECUTION_STATUS_FAILED);
    }

    public void fillIntentLog(AiAgentExecutionLog executionLog, AssistantIntentDecision decision) {
        if (decision == null) {
            return;
        }

        executionLog.setIntent(safeText(decision.intent(), INTENT_KNOWLEDGE_QA));
        executionLog.setConfidence(decision.confidence());
    }

    public void fillResponseLog(AiAgentExecutionLog executionLog, AssistantChatResponse response) {
        if (response == null) {
            return;
        }

        executionLog.setIntent(response.intent());

        if (executionLog.getExecutionStatus() == null || EXECUTION_STATUS_FAILED.equals(executionLog.getExecutionStatus())) {
            executionLog.setExecutionStatus(EXECUTION_STATUS_SUCCESS);
        }

        if (response.ticket() != null) {
            executionLog.setTicketId(response.ticket().ticketId());
            executionLog.setTicketNo(response.ticket().ticketNo());
        }
    }

    public void markKnowledgeQaSuccess(AiAgentExecutionLog executionLog) {
        executionLog.setToolExecuted(false);
        executionLog.setExecutionStatus(EXECUTION_STATUS_SUCCESS);
    }

    public void markPendingConfirm(AiAgentExecutionLog executionLog) {
        executionLog.setToolName(TOOL_CREATE_TICKET);
        executionLog.setToolExecuted(false);
        executionLog.setExecutionStatus(EXECUTION_STATUS_PENDING_CONFIRM);
    }

    public void markUnsupported(AiAgentExecutionLog executionLog, String reason) {
        executionLog.setIntent(INTENT_UNSUPPORTED_REQUEST);
        executionLog.setToolExecuted(false);
        executionLog.setExecutionStatus(EXECUTION_STATUS_UNSUPPORTED);
        executionLog.setErrorMessage(reason);
    }

    public void markCreateTicketRequest(AiAgentExecutionLog executionLog, CreateAiTicketRequest request) {
        executionLog.setToolName(TOOL_CREATE_TICKET);
        executionLog.setToolExecuted(true);
        executionLog.setToolRequest(toLogJson(request));
    }

    public void markCreateTicketSuccess(AiAgentExecutionLog executionLog, CreateAiTicketResponse response) {
        executionLog.setToolResponse(toLogJson(response));
        executionLog.setTicketId(response.ticketId());
        executionLog.setTicketNo(response.ticketNo());
        executionLog.setExecutionStatus(EXECUTION_STATUS_SUCCESS);
    }

    public void markFailed(AiAgentExecutionLog executionLog, Exception ex) {
        executionLog.setExecutionStatus(EXECUTION_STATUS_FAILED);
        executionLog.setErrorMessage(truncate(ex.getMessage(), MAX_ERROR_MESSAGE_LENGTH));
    }

    private String toLogJson(Object value) {
        if (value == null) {
            return null;
        }

        try {
            return truncate(objectMapper.writeValueAsString(value), MAX_JSON_LOG_LENGTH);
        } catch (Exception ex) {
            return truncate(String.valueOf(value), MAX_JSON_LOG_LENGTH);
        }
    }
}