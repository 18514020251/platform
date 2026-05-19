package com.xcvk.platform.ai.service.impl;

import com.xcvk.platform.ai.assembler.AssistantLogAssembler;
import com.xcvk.platform.ai.assembler.AssistantTicketAssembler;
import com.xcvk.platform.ai.model.dto.AssistantChatRequest;
import com.xcvk.platform.ai.model.dto.RagChatRequest;
import com.xcvk.platform.ai.model.entity.AiAgentExecutionLog;
import com.xcvk.platform.ai.model.entity.AiKnowledgeGapTicket;
import com.xcvk.platform.ai.model.internal.AssistantIntentDecision;
import com.xcvk.platform.ai.model.internal.KnowledgeGapTicketDecision;
import com.xcvk.platform.ai.model.internal.TicketScopeValidation;
import com.xcvk.platform.ai.model.vo.AssistantChatResponse;
import com.xcvk.platform.ai.model.vo.AssistantTicketVO;
import com.xcvk.platform.ai.model.vo.RagChatResponse;
import com.xcvk.platform.ai.service.AgentExecutionLogService;
import com.xcvk.platform.ai.service.AssistantService;
import com.xcvk.platform.ai.service.KnowledgeGapTicketDedupService;
import com.xcvk.platform.ai.service.RagChatService;
import com.xcvk.platform.ai.support.AssistantIntentClassifier;
import com.xcvk.platform.ai.support.AssistantTicketScopeValidator;
import com.xcvk.platform.ai.support.KnowledgeGapTicketDecider;
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

import java.util.Optional;

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

    private final KnowledgeGapTicketDecider knowledgeGapTicketDecider;

    private final CreateTicketTool createTicketTool;

    private final QueryTicketTool queryTicketTool;

    private final KnowledgeGapTicketDedupService knowledgeGapTicketDedupService;

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
                () -> handleKnowledgeQa(identity, request, executionLog)
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
     *
     * <p>
     * MVP 新增逻辑：
     * 如果 RAG 因无上下文或低相关性拒答，
     * 则进入“知识缺口转工单”流程。
     * </p>
     */
    private AssistantChatResponse handleKnowledgeQa(CurrentLoginIdentity identity,
                                                    AssistantChatRequest request,
                                                    AiAgentExecutionLog executionLog) {

        RagTraceRecorder traceRecorder = new RagTraceRecorder(RagTraceHolder.get());

        RagChatResponse ragResponse =
                ragChatService.chat(
                        new RagChatRequest(
                                request.question(),
                                request.safeTopK(),
                                request.categoryId(),
                                executionLog.getId()
                        )
                );

        if (!knowledgeGapTicketDecider.canHandle(ragResponse)) {
            logAssembler.markKnowledgeQaSuccess(executionLog);
            return AssistantChatResponse.rag(ragResponse.answer(), ragResponse);
        }

        Optional<AiKnowledgeGapTicket> rawDuplicate =
                traceRecorder.executeNode(
                        RagTraceNodeType.KNOWLEDGE_GAP_TICKET_DEDUP_CHECK,
                        () -> knowledgeGapTicketDedupService.findByRawQuestion(request.question())
                );

        if (rawDuplicate.isPresent()) {
            knowledgeGapTicketDedupService.increaseHitCount(rawDuplicate.get().getId());

            logAssembler.markKnowledgeQaSuccess(executionLog);

            return buildDuplicateTicketResponse(rawDuplicate.get());
        }

        /*
         * 调用 LLM 判断用户原话是否属于企业内部知识缺口。
         */
        KnowledgeGapTicketDecision gapDecision =
                traceRecorder.executeNode(
                        RagTraceNodeType.KNOWLEDGE_GAP_TICKET_DECIDE,
                        () -> knowledgeGapTicketDecider.decide(request.question(), ragResponse)
                );

        Optional<AiKnowledgeGapTicket> normalizedDuplicate =
                traceRecorder.executeNode(
                        RagTraceNodeType.KNOWLEDGE_GAP_TICKET_DEDUP_CHECK,
                        () -> knowledgeGapTicketDedupService.findByNormalizedQuestion(gapDecision)
                );

        if (normalizedDuplicate.isPresent()) {
            knowledgeGapTicketDedupService.increaseHitCount(normalizedDuplicate.get().getId());

            logAssembler.markKnowledgeQaSuccess(executionLog);

            return buildDuplicateTicketResponse(normalizedDuplicate.get());
        }

        /*
         * 后端根据 LLM 决策结果判断是否允许创建工单。
         *
         * 如果不是企业内部问题，或者置信度不足，就返回 RAG 原拒答。
         */
        if (!knowledgeGapTicketDecider.shouldCreateTicket(gapDecision)) {
            logAssembler.markKnowledgeQaSuccess(executionLog);
            return AssistantChatResponse.rag(ragResponse.answer(), ragResponse);
        }

        Boolean duplicateTicketExists =
                traceRecorder.executeNode(
                        RagTraceNodeType.KNOWLEDGE_GAP_TICKET_DEDUP_CHECK,
                        () -> knowledgeGapTicketDecider.duplicateTicketExists(
                                request.question(),
                                gapDecision
                        )
                );

        if (Boolean.TRUE.equals(duplicateTicketExists)) {
            logAssembler.markKnowledgeQaSuccess(executionLog);
            return AssistantChatResponse.rag(ragResponse.answer(), ragResponse);
        }

        /*
         * 第四步：
         * 将知识缺口决策转成已有的 TICKET_CREATE 意图，
         * 然后复用原来的 handleTicketCreate。
         */
        AssistantIntentDecision ticketDecision =
                knowledgeGapTicketDecider.toAssistantIntentDecision(gapDecision);

        AssistantChatResponse ticketResponse =
                traceRecorder.executeNode(
                        RagTraceNodeType.KNOWLEDGE_GAP_CREATE_TICKET,
                        () -> handleTicketCreate(identity, request, ticketDecision, executionLog)
                );

        if (ticketResponse.ticket() != null) {
            knowledgeGapTicketDedupService.recordCreatedTicket(
                    identity.userId(),
                    request.question(),
                    gapDecision,
                    ticketResponse.ticket()
            );
        }

        return ticketResponse;
    }

    private AssistantChatResponse buildDuplicateTicketResponse(AiKnowledgeGapTicket record) {
        AssistantTicketVO ticket = new AssistantTicketVO(
                record.getTicketId(),
                record.getTicketNo(),
                record.getTicketStatus(),
                record.getTicketTypeCode(),
                record.getTicketTitle()
        );

        String answer = """
            这个问题已经存在相似工单，无需重复创建。

            已有关联工单：%s
            当前状态：%s
            工单标题：%s
            """.formatted(
                record.getTicketNo(),
                record.getTicketStatus(),
                record.getTicketTitle()
        );

        return AssistantChatResponse.ticketDuplicate(answer, ticket);
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