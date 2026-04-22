package com.xcvk.platform.workflow.constant;

/**
 * 工单状态常量
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20 16:03
 */
public class TicketStatusConstants {
    private TicketStatusConstants() {
    }

    /**
     * 工单待处理
     */
    public static final String PENDING = "PENDING";

    /**
     * 工单处理中
     */
    public static final String PROCESSING = "PROCESSING";

    /**
     * 工单已解决
     */
    public static final String RESOLVED = "RESOLVED";

    /**
     * 工单已拒绝
     */
    public static final String REJECTED = "REJECTED";

}
