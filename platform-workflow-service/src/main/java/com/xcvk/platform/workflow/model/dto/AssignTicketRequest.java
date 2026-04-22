package com.xcvk.platform.workflow.model.dto;

/**
 * 管理员派发工单请求
 *
 * <p>
 *     管理员通过该接口将工单派发给指定处理人。
 *     管理员指定处理人id和处理人名称，
 *     系统将相关工单信息发送给处理人。
 * </p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-22 15:07
 */
public record AssignTicketRequest(
        Long assigneeId,
        String assigneeName
) {
}
