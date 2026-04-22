package com.xcvk.platform.workflow.model.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新工单状态请求对象
 *
 * <p>该对象用于处理侧更新工单状态。
 * 当前阶段状态更新接口只允许将处理中工单更新为已解决或已拒绝，
 * 并要求同时提交当前状态说明。</p>
 *
 * <p>其中：</p>
 * <ul>
 *     <li>更新为 RESOLVED 时，statusRemark 表示处理结果说明</li>
 *     <li>更新为 REJECTED 时，statusRemark 表示拒绝原因</li>
 * </ul>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-21
 */
public record UpdateTicketStatusRequest(

        /**
         * 目标状态。
         *
         * <p>当前阶段仅允许：
         * RESOLVED / REJECTED。</p>
         */
        @NotBlank(message = "目标状态不能为空")
        String targetStatus,

        /**
         * 当前状态说明。
         *
         * <p>当前阶段更新为已解决或已拒绝时均要求填写，
         * 用于向员工侧展示处理结果说明或拒绝原因。</p>
         */
        @NotBlank(message = "状态说明不能为空")
        String statusRemark

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}