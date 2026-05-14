package com.xcvk.platform.workflow.model.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 管理员派发工单请求。
 *
 * <p>前端只传处理人ID，处理人姓名由后端通过 auth-service 查询，
 * 避免前端伪造处理人姓名。</p>
 */
public record AssignTicketRequest(

        @NotNull(message = "处理人ID不能为空")
        Long assigneeId
) {
}