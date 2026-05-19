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
import com.xcvk.platform.ai.tool.QueryTicketTool;
import com.xcvk.platform.ai.trace.context.RagTraceHolder;
import com.xcvk.platform.ai.trace.enums.RagTraceNodeType;
import com.xcvk.platform.ai.trace.model.RagTraceContext;
import com.xcvk.platform.ai.trace.recorder.RagTraceRecorder;
import com.xcvk.platform.ai.trace.service.RagTraceService;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.id.generator.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.xcvk.platform.ai.constant.AssistantConstants.INTENT_TICKET_CREATE;
import static com.xcvk.platform.ai.constant.AssistantConstants.INTENT_TICKET_QUERY;

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

    private static final String SUCCESS = "SUCCESS";

    private static final String FAILED = "FAILED";

    private final RagChatService ragChatService;

    private final AssistantIntentClassifier intentClassifier;

    private final AssistantTicketScopeValidator ticketScopeValidator;

    private final CreateTicketTool createTicketTool;

    private final QueryTicketTool queryTicketTool;

    private final AssistantLogAssembler logAssembler;

    private final AssistantTicketAssembler ticketAssembler;

    private final AgentExecutionLogService agentExecutionLogService;

    private final SnowflakeIdGenerator idGenerator;

    private final RagTraceService ragTraceService;

    @Override
    public AssistantChatResponse chat(CurrentLoginIdentity identity, AssistantChatRequest request) {
        validateCurrentLoginIdentity(identity);
        validateRequest(request);

        AiAgentExecutionLog executionLog = logAssembler.buildBaseExecutionLog(identity, request);

        RagTraceContext traceContext = new RagTraceContext();
        traceContext.setTraceId(String.valueOf(idGenerator.nextId()));
        traceContext.setQuestion(request.question());
        traceContext.setStartTime(System.currentTimeMillis());
        traceContext.setExecutionLogId(executionLog.getId());

        RagTraceHolder.set(traceContext);

        try {
            RagTraceRecorder traceRecorder = new RagTraceRecorder(traceContext);

            AssistantIntentDecision decision =
                    traceRecorder.executeNode(
                            RagTraceNodeType.INTENT_CLASSIFY,
                            () -> intentClassifier.classify(request.question())
                    );

            logAssembler.fillIntentLog(executionLog, decision);

            AssistantChatResponse response =
                    traceRecorder.executeNode(
                            RagTraceNodeType.ASSISTANT_DISPATCH,
                            () -> dispatch(identity, request, decision, executionLog)
                    );

            traceContext.setStatus(SUCCESS);
            traceContext.setFinalAnswer(response.answer());

            logAssembler.fillResponseLog(executionLog, response);
            return response;
        } catch (Exception ex) {
            traceContext.setStatus(FAILED);

            logAssembler.markFailed(executionLog, ex);
            throw ex;
        } finally {
            traceContext.setEndTime(System.currentTimeMillis());
            traceContext.setTotalLatencyMs(
                    traceContext.getEndTime() - traceContext.getStartTime()
            );

            ragTraceService.save(traceContext);
            RagTraceHolder.clear();

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

        RagTraceRecorder traceRecorder = new RagTraceRecorder(RagTraceHolder.get());

        if (INTENT_TICKET_CREATE.equals(decision.intent())) {
            return traceRecorder.executeNode(
                    RagTraceNodeType.TOOL_CREATE_TICKET,
                    () -> handleTicketCreate(identity, request, decision, executionLog)
            );
        }

        if (INTENT_TICKET_QUERY.equals(decision.intent())) {
            return traceRecorder.executeNode(
                    RagTraceNodeType.TOOL_QUERY_TICKET,
                    () -> handleTicketQuery(identity, request, executionLog)
            );
        }

        return traceRecorder.executeNode(
                RagTraceNodeType.KNOWLEDGE_QA,
                () -> handleKnowledgeQa(request, executionLog)
        );
    }

    /**
     * 处理查询工单。
     */
    private AssistantChatResponse handleTicketQuery(CurrentLoginIdentity identity,
                                                    AssistantChatRequest request,
                                                    AiAgentExecutionLog executionLog) {

        RagTraceRecorder traceRecorder = new RagTraceRecorder(RagTraceHolder.get());

        String answer =
                traceRecorder.executeNode(
                        RagTraceNodeType.TOOL_QUERY_TICKET_EXECUTE,
                        () -> queryTicketTool.query(identity, request, executionLog)
                );

        return AssistantChatResponse.ticketQueried(answer);
    }

    /**
     * 处理知识问答。
     */
    private AssistantChatResponse handleKnowledgeQa(AssistantChatRequest request,
                                                    AiAgentExecutionLog executionLog) {


        RagChatResponse ragResponse =
                ragChatService.chat(
                        new RagChatRequest(
                                request.question(),
                                request.safeTopK(),
                                request.categoryId(),
                                executionLog.getId()
                        )
                );

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

        RagTraceRecorder traceRecorder = new RagTraceRecorder(RagTraceHolder.get());

        TicketScopeValidation validation =
                traceRecorder.executeNode(
                        RagTraceNodeType.TICKET_SCOPE_VALIDATE,
                        () -> ticketScopeValidator.validate(request.question(), decision)
                );

        if (!validation.passed()) {
            logAssembler.markUnsupported(executionLog, validation.reason());
            return AssistantChatResponse.unsupported(validation.reason());
        }

        Boolean autoCreateTicket =
                traceRecorder.executeNode(
                        RagTraceNodeType.TOOL_CREATE_TICKET_CONFIRM_CHECK,
                        request::autoCreateTicketOrTrue
                );

        if (!autoCreateTicket) {
            logAssembler.markPendingConfirm(executionLog);
            return AssistantChatResponse.ticketPendingConfirm(
                    ticketAssembler.buildPendingConfirmAnswer(decision)
            );
        }

        AssistantTicketVO ticket =
                traceRecorder.executeNode(
                        RagTraceNodeType.TOOL_CREATE_TICKET_EXECUTE,
                        () -> createTicketTool.create(
                                identity,
                                request,
                                decision,
                                executionLog
                        )
                );

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