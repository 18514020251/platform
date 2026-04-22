package com.xcvk.platform.workflow.constant;

/**
 * 工单异常文案常量
 *
 * <p>用于统一 workflow 模块中的工单相关异常提示，
 * 避免同类业务错误在不同位置出现不同文案，影响接口一致性与排查效率。</p>
 *
 * <p>当前阶段优先覆盖工单创建、员工侧查询、处理侧查询、接单等核心场景，
 * 后续分派、状态流转等能力继续在此收口。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public final class TicketErrorMessages {

    private TicketErrorMessages() {
    }

    public static final String CREATE_FAILED = "工单创建失败";
    public static final String CREATE_CMD_REQUIRED = "创建工单命令不能为空";
    public static final String TICKET_TYPE_REQUIRED = "工单类型不能为空";
    public static final String TITLE_REQUIRED = "工单标题不能为空";
    public static final String CONTENT_REQUIRED = "工单内容不能为空";
    public static final String CREATOR_NAME_REQUIRED = "创建人姓名不能为空";
    public static final String CREATOR_ID_REQUIRED = "创建人ID不能为空";
    public static final String SOURCE_REQUIRED = "工单来源不能为空";
    public static final String SOURCE_INVALID = "工单来源无效";
    public static final String AI_CREATE_NOT_ALLOWED = "当前工单类型不允许 AI 创建";
    public static final String QUERY_REQUIRED = "查询条件不能为空";
    public static final String TICKET_ID_REQUIRED = "工单ID不能为空";
    public static final String TICKET_NOT_FOUND_OR_NO_PERMISSION = "工单不存在或无权限查看";
    public static final String TICKET_TYPE_NOT_FOUND_OR_DISABLED = "工单类型不存在或状态异常";
    public static final String MANAGE_PERMISSION_DENIED = "当前用户无权限查看处理侧工单";
    public static final String CURRENT_LOGIN_IDENTITY_REQUIRED = "当前登录身份不能为空";

    /**
     * 派单相关异常文案
     */
    public static final String ASSIGN_REQUEST_REQUIRED = "派发请求不能为空";
    public static final String ASSIGNEE_ID_REQUIRED = "处理人ID不能为空";
    public static final String ASSIGNEE_NAME_REQUIRED = "处理人姓名不能为空";
    public static final String ASSIGN_PERMISSION_DENIED = "无权限派发工单";
    public static final String TICKET_STATUS_NOT_ALLOW_ASSIGN = "当前工单状态不允许派发";
    public static final String TICKET_ALREADY_ASSIGNED_OR_STATUS_CHANGED = "工单已被派发或状态已变化";

    /**
     * 接单相关异常文案
     */
    public static final String ACCEPT_PERMISSION_DENIED = "当前用户无权限接单";
    public static final String TICKET_NOT_FOUND = "工单不存在";
    public static final String TICKET_STATUS_NOT_ALLOW_ACCEPT = "当前工单状态不允许接单";
    public static final String TICKET_ALREADY_ASSIGNED = "当前工单已被接单";
    public static final String TICKET_ACCEPT_FAILED = "工单接单失败";
    public static final String TICKET_ALREADY_ACCEPTED_OR_STATUS_CHANGED = "工单已被其他人员接单或当前状态不可接单";

    /**
     * 状态更新相关异常文案
     */
    public static final String STATUS_TARGET_REQUIRED = "目标状态不能为空";
    public static final String STATUS_REMARK_REQUIRED = "状态说明不能为空";
    public static final String STATUS_TARGET_INVALID = "目标状态非法";
    public static final String STATUS_UPDATE_PERMISSION_DENIED = "当前用户无权限更新工单状态";
    public static final String TICKET_STATUS_NOT_ALLOW_UPDATE = "当前工单状态不允许更新";
    public static final String TICKET_STATUS_UPDATE_CONFLICT = "工单状态已变更，请刷新后重试";
}