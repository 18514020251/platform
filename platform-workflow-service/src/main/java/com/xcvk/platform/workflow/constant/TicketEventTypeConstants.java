package com.xcvk.platform.workflow.constant;

/**
 * 工单事件类型常量
 *
 * <p>用于记录工单关键业务动作流水。</p>
 */
public final class TicketEventTypeConstants {

    private TicketEventTypeConstants() {
    }

    /**
     * 创建工单
     */
    public static final String CREATE = "CREATE";

    /**
     * 接单
     */
    public static final String ACCEPT = "ACCEPT";

    /**
     * 派单
     */
    public static final String ASSIGN = "ASSIGN";

    /**
     * 处理完成
     */
    public static final String RESOLVE = "RESOLVE";

    /**
     * 拒绝处理
     */
    public static final String REJECT = "REJECT";
}