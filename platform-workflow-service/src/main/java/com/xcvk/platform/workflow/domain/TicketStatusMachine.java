package com.xcvk.platform.workflow.domain;

import com.xcvk.platform.workflow.constant.TicketStatusConstants;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 工单状态机
 *
 * <p>用于集中维护工单状态流转规则，避免状态判断逻辑散落在 Service 中。</p>
 *
 * <p>当前阶段状态流转规则：</p>
 * <ul>
 *     <li>PENDING -> PROCESSING：接单 / 派单</li>
 *     <li>PROCESSING -> RESOLVED：处理完成</li>
 *     <li>PROCESSING -> REJECTED：拒绝处理</li>
 *     <li>RESOLVED / REJECTED：终态，不允许继续流转</li>
 * </ul>
 */
public final class TicketStatusMachine {

    private TicketStatusMachine() {
    }

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            TicketStatusConstants.PENDING,
            Set.of(TicketStatusConstants.PROCESSING),

            TicketStatusConstants.PROCESSING,
            Set.of(TicketStatusConstants.RESOLVED, TicketStatusConstants.REJECTED),

            TicketStatusConstants.RESOLVED,
            Collections.emptySet(),

            TicketStatusConstants.REJECTED,
            Collections.emptySet()
    );

    /**
     * 处理侧状态更新接口允许提交的目标状态。
     *
     * <p>注意：接单和派单虽然也是状态流转，但它们不是通过状态更新接口完成，
     * 而是分别通过 accept / assign 操作完成。</p>
     */
    private static final Set<String> ALLOWED_PROCESS_RESULT_STATUS = Set.of(
            TicketStatusConstants.RESOLVED,
            TicketStatusConstants.REJECTED
    );

    /**
     * 判断状态是否允许从 fromStatus 流转到 targetStatus。
     *
     * @param fromStatus 当前状态
     * @param targetStatus 目标状态
     * @return true 表示允许流转
     */
    public static boolean canTransfer(String fromStatus, String targetStatus) {
        String from = normalize(fromStatus);
        String target = normalize(targetStatus);

        if (from == null || target == null) {
            return false;
        }

        return ALLOWED_TRANSITIONS
                .getOrDefault(from, Collections.emptySet())
                .contains(target);
    }

    /**
     * 判断是否是当前系统已知状态。
     *
     * @param status 工单状态
     * @return true 表示系统已知
     */
    public static boolean isKnownStatus(String status) {
        String normalizedStatus = normalize(status);
        return normalizedStatus != null && ALLOWED_TRANSITIONS.containsKey(normalizedStatus);
    }

    /**
     * 判断目标状态是否允许通过“处理侧状态更新接口”提交。
     *
     * @param targetStatus 目标状态
     * @return true 表示允许提交
     */
    public static boolean isAllowedProcessResultStatus(String targetStatus) {
        String target = normalize(targetStatus);
        return target != null && ALLOWED_PROCESS_RESULT_STATUS.contains(target);
    }

    private static String normalize(String status) {
        return status == null ? null : status.trim();
    }
}