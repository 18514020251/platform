package com.xcvk.platform.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.io.Serial;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 工单主表
 * </p>
 *
 * @author Programmer
 * @since 2026-04-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wf_ticket")
public class Ticket implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 工单ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 工单编号
     */
    private String ticketNo;

    /**
     * 工单类型ID
     */
    private Long ticketTypeId;

    /**
     * 工单类型编码快照
     */
    private String ticketTypeCode;

    /**
     * 工单类型名称快照
     */
    private String ticketTypeName;

    /**
     * 工单标题
     */
    private String title;

    /**
     * 工单内容
     */
    private String content;

    /**
     * 当前状态
     */
    private String status;

    /**
     * 优先级
     */
    private String priority;

    /**
     * 工单来源：MANUAL/AI_AGENT
     */
    private String source;

    /**
     * 来源关联ID，如AI会话ID
     */
    private String sourceRef;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 创建人名称快照
     */
    private String creatorName;

    /**
     * 当前处理人ID
     */
    private Long assigneeId;

    /**
     * 当前处理人名称快照
     */
    private String assigneeName;

    /**
     * 关闭时间
     */
    private LocalDateTime closedAt;

    /**
     * 当前状态说明/处理备注
     * */
    private String statusRemark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;


}
