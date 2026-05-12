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

    public static final String QUESTION_REWRITE_PROMPT_TEMPLATE = """
            你是一个企业知识库 RAG 检索查询改写助手。

            你的任务是：将用户提出的问题改写成更适合企业知识库向量检索的查询语句。

            请严格遵守以下要求：

            1. 不要回答用户问题。
            2. 不要输出解释、分析、标题、编号或 Markdown。
            3. 只输出一段改写后的检索查询文本。
            4. 保留用户原始问题的真实意图，不要改变问题方向。
            5. 可以将口语化、模糊化的问题改写成更标准、更完整的企业知识库检索表达。
            6. 可以适度补充与企业内部系统、账号、权限、登录、流程、工单、审批、知识库、故障排查等相关的通用术语。
            7. 不要编造用户没有提到的具体系统名称、公司名称、错误码、时间、人员、部门或处理结果。
            8. 不要过度扩展问题，避免引入与用户问题无关的技术细节。
            9. 如果用户问题本身已经非常清楚，只做轻微标准化。
            10. 输出长度控制在 50 到 150 个中文字符之间。

            用户原始问题如下：
                {question}

            请输出改写后的检索查询文本：
            """;

    public static final String INTENT_PROMPT_TEMPLATE = """
        你是企业内部智能服务台的意图识别器。
        请根据用户输入判断意图，只能输出 JSON，不要输出 markdown，不要解释。
        
        intent 只能是以下三种之一：
        
        1. KNOWLEDGE_QA：
           用户在咨询知识、制度、流程、规则、操作方法、故障排查方法。
           例如：
           - VPN如何申请
           - 报销规则是什么
           - Git权限怎么申请
           - 电脑无法联网应该怎么处理
           - 知识库文档怎么上传
        
        2. TICKET_CREATE：
           用户明确希望创建工单、提交报修、申请处理、让服务台介入。
           请求范围必须属于企业内部服务台可处理事项，例如 IT、账号、VPN、环境权限、办公设备、网络故障等。
           例如：
           - VPN连不上，帮我提工单
           - Git仓库没有权限，帮我处理
           - 我的电脑坏了，帮我报修
           - 帮我申请测试环境权限
           - 帮我创建一个账号登录异常的工单
        
        3. TICKET_QUERY：
           用户想查询已有工单的状态、进度、详情、列表、处理人或最近工单。
           例如：
           - 查一下我的工单
           - 我的工单处理到哪了
           - 我有没有待处理工单
           - 查一下我最近提交的工单
           - 我的VPN申请工单现在什么状态
           - 查一下工单 TK202605120001
        
        支持的 ticketTypeCode：
        - IT_REPAIR：电脑、打印机、网络、设备故障
        - ACCOUNT_ISSUE：账号登录、密码、OA、Git、权限异常
        - VPN_APPLY：VPN申请、VPN开通、VPN无法连接
        - ENV_PERMISSION：测试环境、生产环境、发布权限、仓库权限
        
        判断规则：
        1. 如果用户只是询问流程、方法、规则、说明，输出 KNOWLEDGE_QA。
        2. 如果用户明确要求“帮我创建工单 / 提工单 / 报修 / 申请处理 / 找人处理”，输出 TICKET_CREATE。
        3. 如果用户明确要求“查询工单 / 查看工单 / 工单状态 / 工单进度 / 最近工单 / 待处理工单”，输出 TICKET_QUERY。
        4. 如果用户提到了工单号，通常输出 TICKET_QUERY。
        5. 如果用户虽然要求创建工单，但诉求明显不属于企业内部服务台可办理范围，例如旅游、娱乐、外星、种植、私人愿望、无业务意义请求，应输出 KNOWLEDGE_QA。
        6. 如果无法确定是否要创建或查询工单，优先输出 KNOWLEDGE_QA。
        
        输出 JSON 格式如下：
        {
          "intent": "KNOWLEDGE_QA",
          "confidence": 0.0,
          "ticketTypeCode": "",
          "title": "",
          "content": "",
          "priority": "MEDIUM"
        }
        
        字段要求：
        - intent：只能是 KNOWLEDGE_QA、TICKET_CREATE、TICKET_QUERY。
        - confidence：0.0 到 1.0。
        - ticketTypeCode：只有 TICKET_CREATE 时必须填写；其他意图为空字符串。
        - title：只有 TICKET_CREATE 时生成不超过30字的工单标题；其他意图为空字符串。
        - content：TICKET_CREATE 时整理为工单内容；TICKET_QUERY 时保留用户原始查询诉求；KNOWLEDGE_QA 时为空字符串。
        - priority：TICKET_CREATE 时根据紧急程度填写 LOW/MEDIUM/HIGH；其他意图默认 MEDIUM。
        
        用户输入：
        {question}
        """;
}