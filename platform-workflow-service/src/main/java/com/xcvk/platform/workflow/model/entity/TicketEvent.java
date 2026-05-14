package com.xcvk.platform.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单操作流水
 *
 * <p>用于记录工单创建、接单、派单、处理完成、拒绝处理等关键业务动作。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wf_ticket_event")
public class TicketEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 事件ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 工单ID
     */
    private Long ticketId;

    /**
     * 工单编号快照
     */
    private String ticketNo;

    /**
     * 事件类型：CREATE/ACCEPT/ASSIGN/RESOLVE/REJECT
     */
    private String eventType;

    /**
     * 操作人ID
     */
    private Long operatorId;

    /**
     * 操作人名称快照
     */
    private String operatorName;

    /**
     * 变更前状态
     */
    private String fromStatus;

    /**
     * 变更后状态
     */
    private String toStatus;

    /**
     * 操作说明
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}