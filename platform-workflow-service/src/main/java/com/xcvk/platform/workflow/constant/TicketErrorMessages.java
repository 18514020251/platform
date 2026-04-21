package com.xcvk.platform.workflow.constant;

/**
 * 工单错误信息常量
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-21
 */
public final class TicketErrorMessages {

    private TicketErrorMessages() {
    }

    public static final String CREATE_CMD_REQUIRED = "创建工单命令不能为空";
    public static final String TICKET_TYPE_REQUIRED = "工单类型不能为空";
    public static final String TITLE_REQUIRED = "工单标题不能为空";
    public static final String CONTENT_REQUIRED = "工单内容不能为空";
    public static final String CREATOR_NAME_REQUIRED = "创建人不能为空";
    public static final String CREATOR_ID_REQUIRED = "创建人ID不能为空";
    public static final String SOURCE_REQUIRED = "工单来源不能为空";

    public static final String CREATE_FAILED = "工单创建失败";
    public static final String SOURCE_INVALID = "工单来源无效";
    public static final String AI_CREATE_NOT_ALLOWED = "当前工单类型不允许 AI 创建";

    public static final String QUERY_REQUIRED = "查询条件不能为空";
    public static final String TICKET_ID_REQUIRED = "工单ID不能为空";
    public static final String TICKET_NOT_FOUND_OR_NO_PERMISSION = "工单不存在或无权限查看";

    public static final String TICKET_TYPE_NOT_FOUND_OR_DISABLED = "工单类型不存在或状态异常";
}