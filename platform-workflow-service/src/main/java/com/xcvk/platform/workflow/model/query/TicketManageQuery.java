package com.xcvk.platform.workflow.model.query;

import java.io.Serial;
import java.io.Serializable;

/**
 * 处理侧工单查询对象
 *
 * <p>该查询对象用于支持人员或管理员从处理视角分页查询工单，
 * 与员工侧“我的工单查询”职责不同，因此单独建模，避免对象语义混用。</p>
 *
 * <p>当前阶段优先支持处理侧列表的最小筛选能力：
 * 关键字、状态、工单类型、来源、创建人、处理人、
 * 是否仅看我的、是否仅看未分派，以及基础分页参数。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-21
 */
public record TicketManageQuery(
        String keyword,
        String status,
        String ticketTypeCode,
        String source,
        Long creatorId,
        Long assigneeId,
        Boolean mineOnly,
        Boolean unassignedOnly,
        Integer pageNum,
        Integer pageSize
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 兜底页码，避免空值或非法值直接进入分页查询。
     *
     * @return 安全页码
     */
    public int safePageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    /**
     * 兜底分页大小，避免空值或非法值影响列表接口稳定性。
     *
     * @return 安全分页大小
     */
    public int safePageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }

    /**
     * 是否仅查询当前登录人负责的工单。
     *
     * @return true 表示仅看我的
     */
    public boolean mineOnlyOrFalse() {
        return Boolean.TRUE.equals(mineOnly);
    }

    /**
     * 是否仅查询未分派工单。
     *
     * @return true 表示仅看未分派
     */
    public boolean unassignedOnlyOrFalse() {
        return Boolean.TRUE.equals(unassignedOnly);
    }
}