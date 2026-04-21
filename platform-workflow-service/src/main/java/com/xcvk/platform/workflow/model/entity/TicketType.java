package com.xcvk.platform.workflow.model.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 工单类型表
 * </p>
 *
 * @author Programmer
 * @since 2026-04-20
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("wf_ticket_type")
public class TicketType implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 工单类型ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 工单类型编码
     */
    private String typeCode;

    /**
     * 工单类型名称
     */
    private String typeName;

    /**
     * 状态：1启用，0禁用
     */
    private Integer status;

    /**
     * 类型说明
     */
    private String description;

    /**
     * 默认优先级
     */
    private String defaultPriority;

    /**
     * 默认处理角色编码
     */
    private String defaultAssigneeRole;

    /**
     * 是否允许AI创建：1是，0否
     */
    private Integer allowAiCreate;

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
