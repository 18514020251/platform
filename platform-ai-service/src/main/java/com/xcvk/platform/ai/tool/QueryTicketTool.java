package com.xcvk.platform.ai.tool;

import com.xcvk.platform.ai.assembler.AssistantLogAssembler;
import com.xcvk.platform.ai.model.dto.AssistantChatRequest;
import com.xcvk.platform.ai.model.entity.AiAgentExecutionLog;
import com.xcvk.platform.ai.trace.context.RagTraceHolder;
import com.xcvk.platform.ai.trace.enums.RagTraceNodeType;
import com.xcvk.platform.ai.trace.recorder.RagTraceRecorder;
import com.xcvk.platform.api.contract.workflow.client.WorkflowTicketClient;
import com.xcvk.platform.api.contract.workflow.model.QueryAiTicketItem;
import com.xcvk.platform.api.contract.workflow.model.QueryAiTicketRequest;
import com.xcvk.platform.api.contract.workflow.model.QueryAiTicketResponse;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 查询工单 Tool。
 *
 * <p>封装 ai-service 到 workflow-service 的受控工单查询调用。</p>
 */
@Component
@RequiredArgsConstructor
public class QueryTicketTool {

    private static final int DEFAULT_PAGE_NUM = 1;

    private static final int DEFAULT_PAGE_SIZE = 5;

    private static final String SCOPE_CREATED_BY_ME = "CREATED_BY_ME";
    private static final String SCOPE_ASSIGNED_TO_ME = "ASSIGNED_TO_ME";
    private static final String SCOPE_RELATED_TO_ME = "RELATED_TO_ME";

    /**
     * 当前工单编号格式来自 workflow-service：
     * TK + yyyyMMdd + 6位序列。
     *
     * <p>这里稍微放宽，兼容用户少写或复制格式差异。</p>
     */
    private static final Pattern TICKET_NO_PATTERN = Pattern.compile("(?i)TK\\d{10,14}");

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WorkflowTicketClient workflowTicketClient;

    private final AssistantLogAssembler logAssembler;

    /**
     * 查询工单并返回自然语言结果。
     */
    public String query(CurrentLoginIdentity identity,
                        AssistantChatRequest request,
                        AiAgentExecutionLog executionLog) {

        RagTraceRecorder traceRecorder = new RagTraceRecorder(RagTraceHolder.get());

        QueryAiTicketRequest ticketRequest =
                traceRecorder.executeNode(
                        RagTraceNodeType.TOOL_QUERY_TICKET_REQUEST_BUILD,
                        () -> buildQueryRequest(identity, request.question())
                );

        logAssembler.markQueryTicketRequest(executionLog, ticketRequest);

        Result<QueryAiTicketResponse> ticketResult =
                traceRecorder.executeNode(
                        RagTraceNodeType.TOOL_QUERY_TICKET_CALL_WORKFLOW,
                        () -> workflowTicketClient.queryTicketByAi(ticketRequest)
                );

        QueryAiTicketResponse ticketResponse =
                traceRecorder.executeNode(
                        RagTraceNodeType.TOOL_QUERY_TICKET_UNWRAP_RESPONSE,
                        () -> unwrapQueryTicketResult(ticketResult)
                );

        logAssembler.markQueryTicketSuccess(executionLog, ticketResponse);

        return traceRecorder.executeNode(
                RagTraceNodeType.TOOL_QUERY_TICKET_BUILD_ANSWER,
                () -> buildAnswer(ticketResponse, ticketRequest)
        );
    }

    private QueryAiTicketRequest buildQueryRequest(CurrentLoginIdentity identity, String question) {
        String ticketNo = extractTicketNo(question);
        String status = inferStatus(question);
        String scope = inferScope(question);

        Long creatorId = null;
        Long assigneeId = null;

        if (SCOPE_ASSIGNED_TO_ME.equals(scope)) {
            assigneeId = identity.userId();
        } else if (SCOPE_CREATED_BY_ME.equals(scope)) {
            creatorId = identity.userId();
        } else {
            creatorId = identity.userId();
            assigneeId = identity.userId();
        }

        return new QueryAiTicketRequest(
                creatorId,
                assigneeId,
                scope,
                ticketNo,
                status,
                DEFAULT_PAGE_NUM,
                DEFAULT_PAGE_SIZE
        );
    }

    private String inferScope(String question) {
        if (!StringUtils.hasText(question)) {
            return SCOPE_RELATED_TO_ME;
        }

        String normalized = question.toLowerCase().replaceAll("\\s+", "");

        if (containsAny(
                normalized,
                "我负责", "我处理", "我接的", "分配给我", "指派给我",
                "待我处理", "需要我处理", "我名下处理", "需要我处理", "我需要处理"
        )) {
            return SCOPE_ASSIGNED_TO_ME;
        }

        if (containsAny(
                normalized,
                "我提交", "我创建", "我发起", "我提的", "我申请",
                "我报的", "我开的"
        )) {
            return SCOPE_CREATED_BY_ME;
        }

        return SCOPE_RELATED_TO_ME;
    }

    private QueryAiTicketResponse unwrapQueryTicketResult(Result<QueryAiTicketResponse> ticketResult) {
        BizAssert.notNull(ticketResult, ErrorCode.SERVICE_UNAVAILABLE, "工单服务响应为空");

        BizAssert.isTrue(
                ticketResult.getCode() == ErrorCode.SUCCESS.getCode(),
                ErrorCode.BIZ_ERROR,
                ticketResult.getMessage()
        );

        QueryAiTicketResponse ticketResponse = ticketResult.getData();

        BizAssert.notNull(ticketResponse, ErrorCode.BIZ_ERROR, "工单服务未返回查询结果");

        return ticketResponse;
    }

    private String buildAnswer(QueryAiTicketResponse response, QueryAiTicketRequest request) {
        List<QueryAiTicketItem> tickets = response.tickets();

        if (CollectionUtils.isEmpty(tickets)) {
            if (StringUtils.hasText(request.ticketNo())) {
                return "没有查询到与你当前账号相关的工单：" + request.ticketNo()
                        + "。请确认工单号是否正确。";
            }

            if (StringUtils.hasText(request.status())) {
                return "没有查询到你当前账号下状态为【"
                        + statusLabel(request.status())
                        + "】的工单。";
            }

            return "没有查询到你当前账号下的工单。";
        }

        if ("DETAIL".equalsIgnoreCase(response.queryType()) || tickets.size() == 1) {
            return buildDetailAnswer(tickets.get(0));
        }

        return buildListAnswer(tickets, response.total(), request.scope());
    }

    private String buildDetailAnswer(QueryAiTicketItem ticket) {
        StringBuilder builder = new StringBuilder();

        builder.append("查询到工单 ")
                .append(safeText(ticket.ticketNo(), "-"))
                .append("，当前状态为：")
                .append(statusLabel(ticket.status()))
                .append("。\n\n");

        builder.append("标题：").append(safeText(ticket.title(), "-")).append("\n");
        builder.append("类型：").append(safeText(ticket.ticketTypeName(), ticket.ticketTypeCode())).append("\n");
        builder.append("优先级：").append(priorityLabel(ticket.priority())).append("\n");
        builder.append("处理人：").append(safeText(ticket.assigneeName(), "暂未分配")).append("\n");
        builder.append("创建时间：").append(formatTime(ticket.createdAt())).append("\n");
        builder.append("更新时间：").append(formatTime(ticket.updatedAt())).append("\n");

        if (StringUtils.hasText(ticket.statusRemark())) {
            builder.append("状态说明：").append(ticket.statusRemark()).append("\n");
        }

        return builder.toString();
    }

    private String buildListAnswer(List<QueryAiTicketItem> tickets, Integer total, String scope) {
        StringBuilder builder = new StringBuilder();

        builder.append(scopeListTitle(scope));
        if (total != null) {
            builder.append("，共查询到 ").append(total).append(" 条");
        }
        builder.append("：\n\n");

        for (int i = 0; i < tickets.size(); i++) {
            QueryAiTicketItem ticket = tickets.get(i);

            builder.append(i + 1)
                    .append(". ")
                    .append(safeText(ticket.ticketNo(), "-"))
                    .append("｜")
                    .append(safeText(ticket.title(), "-"))
                    .append("｜")
                    .append(statusLabel(ticket.status()))
                    .append("｜优先级：")
                    .append(priorityLabel(ticket.priority()))
                    .append("｜处理人：")
                    .append(safeText(ticket.assigneeName(), "暂未分配"))
                    .append("\n")
                    .append("   更新时间：")
                    .append(formatTime(ticket.updatedAt()))
                    .append("\n\n");
        }

        return builder.toString().trim();
    }

    private String scopeListTitle(String scope) {
        if (SCOPE_ASSIGNED_TO_ME.equals(scope)) {
            return "你负责处理的工单如下";
        }

        if (SCOPE_CREATED_BY_ME.equals(scope)) {
            return "你提交的工单如下";
        }

        return "与你相关的工单如下";
    }

    private String extractTicketNo(String question) {
        if (!StringUtils.hasText(question)) {
            return null;
        }

        Matcher matcher = TICKET_NO_PATTERN.matcher(question);

        if (matcher.find()) {
            return matcher.group().toUpperCase();
        }

        return null;
    }

    private String inferStatus(String question) {
        if (!StringUtils.hasText(question)) {
            return null;
        }

        String normalized = question.toLowerCase().replaceAll("\\s+", "");

        if (containsAny(normalized, "待处理", "待受理", "未处理", "还没处理")) {
            return "PENDING";
        }

        if (containsAny(normalized, "处理中", "处理中的", "正在处理")) {
            return "PROCESSING";
        }

        if (containsAny(normalized, "已解决", "已完成", "处理好了", "完成的", "解决的")) {
            return "RESOLVED";
        }

        if (containsAny(normalized, "已拒绝", "被拒绝", "拒绝的", "不通过")) {
            return "REJECTED";
        }

        return null;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String statusLabel(String status) {
        if (!StringUtils.hasText(status)) {
            return "-";
        }

        return switch (status) {
            case "PENDING" -> "待处理";
            case "PROCESSING" -> "处理中";
            case "RESOLVED" -> "已解决";
            case "REJECTED" -> "已拒绝";
            default -> status;
        };
    }

    private String priorityLabel(String priority) {
        if (!StringUtils.hasText(priority)) {
            return "-";
        }

        return switch (priority) {
            case "LOW" -> "低";
            case "MEDIUM" -> "中";
            case "HIGH" -> "高";
            default -> priority;
        };
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return "-";
        }

        return DATE_TIME_FORMATTER.format(time);
    }

    private String safeText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }
}