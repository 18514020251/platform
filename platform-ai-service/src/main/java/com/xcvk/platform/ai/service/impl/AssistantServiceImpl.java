package com.xcvk.platform.ai.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcvk.platform.ai.model.dto.AssistantChatRequest;
import com.xcvk.platform.ai.model.dto.RagChatRequest;
import com.xcvk.platform.ai.model.entity.AiAgentExecutionLog;
import com.xcvk.platform.ai.model.vo.AssistantChatResponse;
import com.xcvk.platform.ai.model.vo.AssistantTicketVO;
import com.xcvk.platform.ai.model.vo.RagChatResponse;
import com.xcvk.platform.ai.service.AgentExecutionLogService;
import com.xcvk.platform.ai.service.AssistantService;
import com.xcvk.platform.ai.service.RagChatService;
import com.xcvk.platform.api.contract.workflow.client.WorkflowTicketClient;
import com.xcvk.platform.api.contract.workflow.model.CreateAiTicketRequest;
import com.xcvk.platform.api.contract.workflow.model.CreateAiTicketResponse;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 智能助手服务实现。
 *
 * <p>当前实现采用“LLM 意图识别 + 后端白名单 Tool 编排”的可控 Agent 方案：</p>
 * <ul>
 *     <li>模型只负责识别意图和抽取参数</li>
 *     <li>是否调用 Tool 由后端代码决定</li>
 *     <li>真正创建工单时，只能调用后端预定义的 workflow Tool</li>
 * </ul>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-08
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {

    private static final String INTENT_KNOWLEDGE_QA = "KNOWLEDGE_QA";

    private static final String INTENT_TICKET_CREATE = "TICKET_CREATE";

    private static final String DEFAULT_PRIORITY = "MEDIUM";

    private static final String DEFAULT_TICKET_TYPE = "IT_REPAIR";

    private static final String SOURCE_REF_PREFIX = "ai-session-";

    private static final String TOOL_CREATE_TICKET = "createTicket";

    private static final String EXECUTION_STATUS_SUCCESS = "SUCCESS";

    private static final String EXECUTION_STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";

    private static final String EXECUTION_STATUS_UNSUPPORTED = "UNSUPPORTED";

    private static final String EXECUTION_STATUS_FAILED = "FAILED";

    private static final int MAX_JSON_LOG_LENGTH = 4000;

    private static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    private static final String INTENT_PROMPT_TEMPLATE = """
            你是企业内部智能服务台的意图识别器。
            请根据用户输入判断意图，只能输出 JSON，不要输出 markdown，不要解释。
            
            支持的 intent：
            1. KNOWLEDGE_QA：知识问答、制度咨询、流程说明，例如“VPN如何申请”“报销规则是什么”
            2. TICKET_CREATE：用户明确需要报修、申请权限、处理故障、创建企业内部服务台工单，
               且问题属于 IT、账号、VPN、环境权限、办公设备、网络故障等企业内部支持范围。
               例如“VPN连不上，帮我提工单”“Git仓库没有权限，帮我处理”
            
            支持的 ticketTypeCode：
            - IT_REPAIR：电脑、打印机、网络、设备故障
            - ACCOUNT_ISSUE：账号登录、密码、OA、Git、权限异常
            - VPN_APPLY：VPN申请、VPN开通、VPN无法连接
            - ENV_PERMISSION：测试环境、生产环境、发布权限、仓库权限
            
            输出 JSON 格式：
            {
              "intent": "KNOWLEDGE_QA 或 TICKET_CREATE",
              "confidence": 0.0到1.0,
              "ticketTypeCode": "工单类型编码，非工单意图时为空字符串",
              "title": "如果是工单，生成不超过30字的工单标题，否则为空字符串",
              "content": "如果是工单，整理用户问题作为工单内容，否则为空字符串",
              "priority": "LOW/MEDIUM/HIGH"
            }
            
            用户输入：
            {question}
            """;

    private static final String UNSUPPORTED_TICKET_ANSWER = "该请求不属于企业内部服务台可办理范围，暂不支持创建工单。"
            + "请提交与 IT、账号、VPN、权限、办公设备、网络故障或企业流程相关的问题。";

    private static final Set<String> SUPPORTED_TICKET_TYPE_CODES = Set.of(
            "IT_REPAIR",
            "ACCOUNT_ISSUE",
            "VPN_APPLY",
            "ENV_PERMISSION"
    );

    /**
     * 企业服务台可办理范围关键词。
     *
     * <p>Agent Tool 调用前必须经过后端业务范围校验，避免用户只要说“创建工单”
     * 就能把任意离谱诉求写入工单系统。</p>
     */
    private static final String[] BUSINESS_TICKET_KEYWORDS = {
            // IT / 账号 / 权限
            "vpn", "远程办公",
            "账号", "密码", "登录", "登陆", "oa", "git", "邮箱", "权限", "无权限",
            "测试环境", "生产环境", "发布权限", "仓库权限", "环境权限",
            "申请权限", "开通权限", "权限申请",

            // 办公设备 / 资产 / 采购
            "电脑", "打印机", "网络", "蓝屏", "设备", "鼠标", "键盘", "显示器",
            "资产", "采购", "购买", "买", "领用", "办公设备", "办公用品",

            // 故障现象
            "无法访问", "访问不了", "连不上", "打不开", "报错", "故障", "坏了"
    };

    /**
     * 明显不属于企业内部服务台的离谱或越界诉求关键词。
     *
     * <p>这里只做第一层硬拦截，后续可以升级为“工单范围分类器”。</p>
     */
    private static final String[] OUT_OF_SCOPE_TICKET_PATTERNS = {
            "去火星",
            "到火星",
            "前往火星",
            "火星种西瓜",
            "去月球",
            "到月球",
            "前往月球",
            "宇宙飞船",
            "穿越",
            "彩票",
            "算命"
    };

    private final RagChatService ragChatService;

    private final WorkflowTicketClient workflowTicketClient;

    private final ChatModel chatModel;

    private final ObjectMapper objectMapper;

    private final AgentExecutionLogService agentExecutionLogService;

    @Override
    public AssistantChatResponse chat(CurrentLoginIdentity identity, AssistantChatRequest request) {
        validateCurrentLoginIdentity(identity);
        validateRequest(request);

        AiAgentExecutionLog executionLog = buildBaseExecutionLog(identity, request);

        try {
            AssistantIntentDecision decision = classifyIntent(request.question());

            fillIntentLog(executionLog, decision);

            AssistantChatResponse response;
            if (INTENT_TICKET_CREATE.equals(decision.intent())) {
                response = handleTicketCreate(identity, request, decision, executionLog);
            } else {
                response = handleKnowledgeQa(request, executionLog);
            }

            fillResponseLog(executionLog, response);
            return response;
        } catch (Exception ex) {
            executionLog.setExecutionStatus(EXECUTION_STATUS_FAILED);
            executionLog.setErrorMessage(truncate(ex.getMessage(), MAX_ERROR_MESSAGE_LENGTH));
            throw ex;
        } finally {
            agentExecutionLogService.saveSafely(executionLog);
        }
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

        executionLog.setToolExecuted(false);
        executionLog.setExecutionStatus(EXECUTION_STATUS_SUCCESS);

        return AssistantChatResponse.rag(ragResponse.answer(), ragResponse);
    }

    /**
     * 处理创建工单。
     *
     * <p>创建人信息来自 Sa-Token 当前登录身份，不允许前端传入。</p>
     *
     * <p>注意：模型只负责识别意图，是否允许调用 createTicket Tool
     * 必须由后端根据业务范围进行最终校验。</p>
     */
    private AssistantChatResponse handleTicketCreate(CurrentLoginIdentity identity,
                                                     AssistantChatRequest request,
                                                     AssistantIntentDecision decision,
                                                     AiAgentExecutionLog executionLog){
        TicketScopeValidation scopeValidation = validateTicketScope(request.question(), decision);
        if (!scopeValidation.passed()) {
            executionLog.setIntent("UNSUPPORTED_REQUEST");
            executionLog.setToolExecuted(false);
            executionLog.setExecutionStatus(EXECUTION_STATUS_UNSUPPORTED);
            executionLog.setErrorMessage(scopeValidation.reason());

            return AssistantChatResponse.unsupported(scopeValidation.reason());
        }

        if (!request.autoCreateTicketOrTrue()) {
            String answer = "我已识别到这是一个需要创建工单的问题，建议创建【"
                    + safeText(decision.title(), "待处理问题")
                    + "】工单。请确认后再提交。";

            executionLog.setToolName(TOOL_CREATE_TICKET);
            executionLog.setToolExecuted(false);
            executionLog.setExecutionStatus(EXECUTION_STATUS_PENDING_CONFIRM);

            return AssistantChatResponse.ticketPendingConfirm(answer);
        }

        String ticketTypeCode = safeText(decision.ticketTypeCode(), DEFAULT_TICKET_TYPE);
        String title = safeText(decision.title(), buildDefaultTitle(request.question()));
        String content = safeText(decision.content(), request.question());
        String priority = safeText(decision.priority(), DEFAULT_PRIORITY);
        String sourceRef = SOURCE_REF_PREFIX + UUID.randomUUID();

        CreateAiTicketRequest ticketRequest = new CreateAiTicketRequest(
                identity.userId(),
                resolveCreatorName(identity),
                ticketTypeCode,
                title,
                content,
                priority,
                sourceRef
        );

        executionLog.setToolName(TOOL_CREATE_TICKET);
        executionLog.setToolExecuted(true);
        executionLog.setToolRequest(toLogJson(ticketRequest));

        Result<CreateAiTicketResponse> ticketResult = workflowTicketClient.createTicketByAi(ticketRequest);

        CreateAiTicketResponse ticketResponse = unwrapCreateTicketResult(ticketResult);

        executionLog.setToolResponse(toLogJson(ticketResponse));
        executionLog.setTicketId(ticketResponse.ticketId());
        executionLog.setTicketNo(ticketResponse.ticketNo());
        executionLog.setExecutionStatus(EXECUTION_STATUS_SUCCESS);

        AssistantTicketVO ticket = new AssistantTicketVO(
                ticketResponse.ticketId(),
                ticketResponse.ticketNo(),
                ticketResponse.status(),
                ticketTypeCode,
                title
        );

        String answer = "已为你创建工单："
                + ticketResponse.ticketNo()
                + "，当前状态为 "
                + ticketResponse.status()
                + "。工单类型："
                + ticketTypeCode
                + "。";

        return AssistantChatResponse.ticketCreated(answer, ticket);
    }

    /**
     * 构建基础执行日志。
     */
    private AiAgentExecutionLog buildBaseExecutionLog(CurrentLoginIdentity identity, AssistantChatRequest request) {
        return new AiAgentExecutionLog()
                .setUserId(identity.userId())
                .setUsername(resolveCreatorName(identity))
                .setQuestion(request.question())
                .setToolExecuted(false)
                .setExecutionStatus(EXECUTION_STATUS_FAILED);
    }

    /**
     * 填充意图识别日志。
     */
    private void fillIntentLog(AiAgentExecutionLog executionLog, AssistantIntentDecision decision) {
        if (decision == null) {
            return;
        }

        executionLog.setIntent(safeText(decision.intent(), INTENT_KNOWLEDGE_QA));
        executionLog.setConfidence(decision.confidence());
    }

    /**
     * 根据最终响应补充日志。
     */
    private void fillResponseLog(AiAgentExecutionLog executionLog, AssistantChatResponse response) {
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

    /**
     * 对象转日志 JSON。
     */
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

    /**
     * 截断日志文本。
     */
    private String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }

    /**
     * 校验工单请求是否属于企业服务台可办理范围。
     *
     * <p>这是 Agent Tool 调用前的后端安全闸门：
     * 即使大模型识别为 TICKET_CREATE，也不能直接调用 workflow-service。</p>
     */
    private TicketScopeValidation validateTicketScope(String question, AssistantIntentDecision decision) {
        String ticketTypeCode = safeText(decision.ticketTypeCode(), DEFAULT_TICKET_TYPE);

        if (!SUPPORTED_TICKET_TYPE_CODES.contains(ticketTypeCode)) {
            return TicketScopeValidation.reject(UNSUPPORTED_TICKET_ANSWER);
        }

        String normalizedQuestion = normalizeForKeywordMatch(question);

        boolean hasBusinessKeyword = containsAny(normalizedQuestion, BUSINESS_TICKET_KEYWORDS);
        boolean hasOutOfScopePattern = containsAny(normalizedQuestion, OUT_OF_SCOPE_TICKET_PATTERNS);

        if (hasBusinessKeyword) {
            return TicketScopeValidation.pass();
        }

        if (hasOutOfScopePattern) {
            return TicketScopeValidation.reject(UNSUPPORTED_TICKET_ANSWER);
        }

        return TicketScopeValidation.reject(UNSUPPORTED_TICKET_ANSWER);
    }

    /**
     * 工单范围校验结果。
     */
    private record TicketScopeValidation(
            boolean passed,
            String reason
    ) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        static TicketScopeValidation pass() {
            return new TicketScopeValidation(true, null);
        }

        static TicketScopeValidation reject(String reason) {
            return new TicketScopeValidation(false, reason);
        }
    }

    /**
     * 关键词匹配前的文本归一化。
     */
    private String normalizeForKeywordMatch(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        return text.toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    /**
     * 解包 workflow-service 创建工单响应。
     *
     * <p>workflow-service 统一返回 Result 包装结构，
     * Agent Tool 需要显式校验 code 和 data，避免出现 Feign 解析成功但业务字段为空的问题。</p>
     */
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

    /**
     * 校验当前登录身份。
     */
    private void validateCurrentLoginIdentity(CurrentLoginIdentity identity) {
        BizAssert.notNull(identity, ErrorCode.PARAM_INVALID, "当前登录身份不能为空");
        BizAssert.notNull(identity.userId(), ErrorCode.PARAM_INVALID, "当前登录用户ID不能为空");
    }

    /**
     * 解析创建人名称。
     *
     * <p>优先使用真实姓名，真实姓名为空时回退到 username，
     * 避免工单 creatorName 为空。</p>
     */
    private String resolveCreatorName(CurrentLoginIdentity identity) {
        if (StringUtils.hasText(identity.realName())) {
            return identity.realName().trim();
        }

        if (StringUtils.hasText(identity.username())) {
            return identity.username().trim();
        }

        return "用户" + identity.userId();
    }

    /**
     * 使用大模型识别意图，失败时降级为关键词规则。
     */
    private AssistantIntentDecision classifyIntent(String question) {
        try {
            String prompt = INTENT_PROMPT_TEMPLATE.replace("{question}", question);
            String modelOutput = chatModel.chat(prompt);
            String json = extractJson(modelOutput);

            AssistantIntentDecision decision = objectMapper.readValue(json, AssistantIntentDecision.class);
            return normalizeDecision(question, decision);
        } catch (Exception ex) {
            log.warn("LLM意图识别失败，降级为关键词规则，question={}", question, ex);
            return fallbackClassify(question);
        }
    }

    /**
     * 兜底意图识别规则。
     */
    private AssistantIntentDecision fallbackClassify(String question) {
        String lower = question.toLowerCase(Locale.ROOT);

        boolean explicitTicket = containsAny(
                lower,
                "工单", "提单", "报修", "帮我处理", "帮我开通", "创建", "申请"
        );

        boolean issue = containsAny(
                lower,
                "不能", "无法", "连不上", "登录不上", "报错", "故障", "坏了", "蓝屏", "没权限", "访问不了"
        );

        if (explicitTicket || issue) {
            String ticketTypeCode = inferTicketType(question);
            return new AssistantIntentDecision(
                    INTENT_TICKET_CREATE,
                    0.65,
                    ticketTypeCode,
                    buildDefaultTitle(question),
                    question,
                    inferPriority(question)
            );
        }

        return new AssistantIntentDecision(
                INTENT_KNOWLEDGE_QA,
                0.60,
                "",
                "",
                "",
                DEFAULT_PRIORITY
        );
    }

    /**
     * 修正规范化模型输出。
     */
    private AssistantIntentDecision normalizeDecision(String question, AssistantIntentDecision decision) {
        if (decision == null || !StringUtils.hasText(decision.intent())) {
            return fallbackClassify(question);
        }

        String intent = decision.intent().trim();

        if (!INTENT_TICKET_CREATE.equals(intent)) {
            return new AssistantIntentDecision(
                    INTENT_KNOWLEDGE_QA,
                    decision.confidence(),
                    "",
                    "",
                    "",
                    DEFAULT_PRIORITY
            );
        }

        String ticketTypeCode = StringUtils.hasText(decision.ticketTypeCode())
                ? decision.ticketTypeCode().trim()
                : inferTicketType(question);

        String title = StringUtils.hasText(decision.title())
                ? decision.title().trim()
                : buildDefaultTitle(question);

        String content = StringUtils.hasText(decision.content())
                ? decision.content().trim()
                : question;

        String priority = StringUtils.hasText(decision.priority())
                ? decision.priority().trim()
                : inferPriority(question);

        return new AssistantIntentDecision(
                INTENT_TICKET_CREATE,
                decision.confidence(),
                ticketTypeCode,
                title,
                content,
                priority
        );
    }

    /**
     * 从模型输出中抽取 JSON。
     */
    private String extractJson(String text) {
        if (!StringUtils.hasText(text)) {
            return "{}";
        }

        String cleaned = text.trim()
                .replace("```json", "")
                .replace("```", "")
                .trim();

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }

        return cleaned;
    }

    private String inferTicketType(String question) {
        String lower = question.toLowerCase(Locale.ROOT);

        if (containsAny(lower, "vpn", "远程办公")) {
            return "VPN_APPLY";
        }

        if (containsAny(lower, "账号", "密码", "登录", "oa", "git", "权限", "无权限")) {
            return "ACCOUNT_ISSUE";
        }

        if (containsAny(lower, "测试环境", "生产环境", "发布权限", "仓库权限", "环境权限")) {
            return "ENV_PERMISSION";
        }

        if (containsAny(lower, "资产", "采购", "购买", "买", "领用", "显示器", "办公设备", "办公用品")) {
            return "IT_REPAIR";
        }

        if (containsAny(lower, "电脑", "打印机", "网络", "蓝屏", "设备", "鼠标", "键盘")) {
            return "IT_REPAIR";
        }

        return DEFAULT_TICKET_TYPE;
    }

    private String inferPriority(String question) {
        String lower = question.toLowerCase(Locale.ROOT);

        if (containsAny(lower, "生产", "紧急", "严重", "无法办公", "线上")) {
            return "HIGH";
        }

        if (containsAny(lower, "咨询", "了解", "低优先级")) {
            return "LOW";
        }

        return DEFAULT_PRIORITY;
    }

    private String buildDefaultTitle(String question) {
        String normalized = question == null ? "待处理问题" : question.trim().replaceAll("\\s+", " ");

        if (normalized.length() <= 30) {
            return normalized;
        }

        return normalized.substring(0, 30);
    }

    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }

        String normalizedText = normalizeForKeywordMatch(text);

        for (String keyword : keywords) {
            String normalizedKeyword = normalizeForKeywordMatch(keyword);
            if (StringUtils.hasText(normalizedKeyword) && normalizedText.contains(normalizedKeyword)) {
                return true;
            }
        }

        return false;
    }

    private String safeText(String value, String fallback) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return fallback;
    }

    private void validateRequest(AssistantChatRequest request) {
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, "智能助手请求不能为空");
        BizAssert.hasText(request.question(), ErrorCode.PARAM_INVALID, "问题不能为空");
    }

    /**
     * Agent 意图识别结果。
     */
    private record AssistantIntentDecision(
            String intent,
            Double confidence,
            String ticketTypeCode,
            String title,
            String content,
            String priority
    ) implements Serializable {

        @Serial
        private static final long serialVersionUID = 1L;
    }
}