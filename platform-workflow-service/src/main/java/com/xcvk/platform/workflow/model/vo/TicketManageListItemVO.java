package com.xcvk.platform.workflow.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 处理侧工单列表项
 *
 * <p>该对象面向支持人员与管理员的处理工作台列表展示，
 * 相比员工侧列表，额外保留创建人和处理人相关字段，
 * 便于处理侧快速判断工单归属与当前处理状态。</p>
 *
 * <p>当前阶段只返回处理列表所需最小字段，
 * 后续如增加受理时间、关闭时间、处理结果摘要等字段，
 * 可继续在该对象上扩展，而不影响员工侧列表对象。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-21
 */
public record TicketManageListItemVO(
        Long ticketId,
        String ticketNo,
        String ticketTypeCode,
        String ticketTypeName,
        String title,
        String status,
        String priority,
        String source,
        Long creatorId,
        String creatorName,
        Long assigneeId,
        String assigneeName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}