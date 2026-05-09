package com.xcvk.platform.ai.constant;

import java.util.Set;

/**
 * Assistant 常量。
 *
 * @author Programmer
 * @version 1.1
 * @date 2026-05-09
 */
public final class AssistantConstants {

    private AssistantConstants() {
    }

    public static final String INTENT_KNOWLEDGE_QA = "KNOWLEDGE_QA";

    public static final String INTENT_TICKET_CREATE = "TICKET_CREATE";

    public static final String INTENT_UNSUPPORTED_REQUEST = "UNSUPPORTED_REQUEST";

    public static final String TOOL_CREATE_TICKET = "createTicket";

    public static final String TICKET_TYPE_IT_REPAIR = "IT_REPAIR";

    public static final String TICKET_TYPE_ACCOUNT_ISSUE = "ACCOUNT_ISSUE";

    public static final String TICKET_TYPE_VPN_APPLY = "VPN_APPLY";

    public static final String TICKET_TYPE_ENV_PERMISSION = "ENV_PERMISSION";

    public static final String PRIORITY_LOW = "LOW";

    public static final String PRIORITY_MEDIUM = "MEDIUM";

    public static final String PRIORITY_HIGH = "HIGH";

    public static final String DEFAULT_PRIORITY = PRIORITY_MEDIUM;

    public static final String DEFAULT_TICKET_TYPE = TICKET_TYPE_IT_REPAIR;

    public static final String SOURCE_REF_PREFIX = "ai-session-";

    public static final String EXECUTION_STATUS_SUCCESS = "SUCCESS";

    public static final String EXECUTION_STATUS_PENDING_CONFIRM = "PENDING_CONFIRM";

    public static final String EXECUTION_STATUS_UNSUPPORTED = "UNSUPPORTED";

    public static final String EXECUTION_STATUS_FAILED = "FAILED";

    public static final int MAX_JSON_LOG_LENGTH = 4000;

    public static final int MAX_ERROR_MESSAGE_LENGTH = 1000;

    public static final String UNSUPPORTED_TICKET_ANSWER = "该请求不属于企业内部服务台可办理范围，暂不支持创建工单。"
            + "请提交与 IT、账号、VPN、权限、办公设备、网络故障或企业流程相关的问题。";

    private static final Set<String> SUPPORTED_TICKET_TYPE_CODES = Set.of(
            TICKET_TYPE_IT_REPAIR,
            TICKET_TYPE_ACCOUNT_ISSUE,
            TICKET_TYPE_VPN_APPLY,
            TICKET_TYPE_ENV_PERMISSION
    );

    private static final String[] BUSINESS_TICKET_KEYWORDS = {
            "vpn", "远程办公",
            "账号", "密码", "登录", "登陆", "oa", "git", "邮箱", "权限", "无权限",
            "测试环境", "生产环境", "发布权限", "仓库权限", "环境权限",
            "申请权限", "开通权限", "权限申请",

            "电脑", "打印机", "网络", "蓝屏", "设备", "鼠标", "键盘", "显示器",
            "资产", "采购", "购买", "买", "领用", "办公设备", "办公用品",

            "无法访问", "访问不了", "连不上", "打不开", "报错", "故障", "坏了"
    };

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

    public static boolean isSupportedTicketType(String ticketTypeCode) {
        return SUPPORTED_TICKET_TYPE_CODES.contains(ticketTypeCode);
    }

    public static String[] businessTicketKeywords() {
        return BUSINESS_TICKET_KEYWORDS.clone();
    }

    public static String[] outOfScopeTicketPatterns() {
        return OUT_OF_SCOPE_TICKET_PATTERNS.clone();
    }
}