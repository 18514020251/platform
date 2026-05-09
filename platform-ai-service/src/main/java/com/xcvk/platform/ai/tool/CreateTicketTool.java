package com.xcvk.platform.ai.tool;

import com.xcvk.platform.ai.assembler.AssistantLogAssembler;
import com.xcvk.platform.ai.model.dto.AssistantChatRequest;
import com.xcvk.platform.ai.model.entity.AiAgentExecutionLog;
import com.xcvk.platform.ai.model.internal.AssistantIntentDecision;
import com.xcvk.platform.ai.model.vo.AssistantTicketVO;
import com.xcvk.platform.ai.support.AssistantIdentityResolver;
import com.xcvk.platform.api.contract.workflow.client.WorkflowTicketClient;
import com.xcvk.platform.api.contract.workflow.model.CreateAiTicketRequest;
import com.xcvk.platform.api.contract.workflow.model.CreateAiTicketResponse;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static com.xcvk.platform.ai.constant.AssistantConstants.*;
import static com.xcvk.platform.ai.support.AssistantTextUtils.buildDefaultTitle;
import static com.xcvk.platform.ai.support.AssistantTextUtils.safeText;

/**
 * 创建工单 Tool。
 *
 * <p>封装 ai-service 到 workflow-service 的受控 Tool 调用。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-09
 */
@Component
@RequiredArgsConstructor
public class CreateTicketTool {

    private final WorkflowTicketClient workflowTicketClient;

    private final AssistantIdentityResolver identityResolver;

    private final AssistantLogAssembler logAssembler;

    /**
     * 创建工单。
     *
     * @param identity 当前登录身份
     * @param request Assistant 请求
     * @param decision 意图识别结果
     * @param executionLog Agent 执行日志
     * @return 工单结果
     */
    public AssistantTicketVO create(CurrentLoginIdentity identity,
                                    AssistantChatRequest request,
                                    AssistantIntentDecision decision,
                                    AiAgentExecutionLog executionLog) {
        String ticketTypeCode = safeText(decision.ticketTypeCode(), DEFAULT_TICKET_TYPE);
        String title = safeText(decision.title(), buildDefaultTitle(request.question()));
        String content = safeText(decision.content(), request.question());
        String priority = safeText(decision.priority(), DEFAULT_PRIORITY);
        String sourceRef = SOURCE_REF_PREFIX + UUID.randomUUID();

        CreateAiTicketRequest ticketRequest = new CreateAiTicketRequest(
                identity.userId(),
                identityResolver.resolveCreatorName(identity),
                ticketTypeCode,
                title,
                content,
                priority,
                sourceRef
        );

        logAssembler.markCreateTicketRequest(executionLog, ticketRequest);

        Result<CreateAiTicketResponse> ticketResult = workflowTicketClient.createTicketByAi(ticketRequest);
        CreateAiTicketResponse ticketResponse = unwrapCreateTicketResult(ticketResult);

        logAssembler.markCreateTicketSuccess(executionLog, ticketResponse);

        return new AssistantTicketVO(
                ticketResponse.ticketId(),
                ticketResponse.ticketNo(),
                ticketResponse.status(),
                ticketTypeCode,
                title
        );
    }

    private CreateAiTicketResponse unwrapCreateTicketResult(Result<CreateAiTicketResponse> ticketResult) {
        BizAssert.notNull(ticketResult, ErrorCode.SERVICE_UNAVAILABLE, "工单服务响应为空");

        BizAssert.isTrue(
                ticketResult.getCode() == ErrorCode.SUCCESS.getCode(),
                ErrorCode.BIZ_ERROR,
                ticketResult.getMessage()
        );

        CreateAiTicketResponse ticketResponse = ticketResult.getData();

        BizAssert.notNull(ticketResponse, ErrorCode.BIZ_ERROR, "工单服务未返回创建结果");
        BizAssert.notNull(ticketResponse.ticketId(), ErrorCode.BIZ_ERROR, "工单ID不能为空");
        BizAssert.hasText(ticketResponse.ticketNo(), ErrorCode.BIZ_ERROR, "工单编号不能为空");
        BizAssert.hasText(ticketResponse.status(), ErrorCode.BIZ_ERROR, "工单状态不能为空");

        return ticketResponse;
    }
}