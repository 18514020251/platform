package com.xcvk.platform.ai.constant;

/**
 * Assistant Prompt 模板。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-09
 */
public final class AssistantPromptTemplates {

    private AssistantPromptTemplates() {
    }

    public static final String INTENT_PROMPT_TEMPLATE = """
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
            
            如果用户虽然要求创建工单，但诉求明显不属于企业内部服务台可办理范围，
            例如旅游、娱乐、外星、种植、私人愿望、无业务意义请求，应输出 KNOWLEDGE_QA。
            
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
}