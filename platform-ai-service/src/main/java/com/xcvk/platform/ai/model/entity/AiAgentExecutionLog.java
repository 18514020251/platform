package com.xcvk.platform.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI Agent 执行日志。
 *
 * <p>用于记录 Assistant 意图识别、Tool 调用、工单创建结果和异常信息，
 * 方便后续审计、排障和效果分析。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-09
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_agent_execution_log")
public class AiAgentExecutionLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称快照
     */
    private String username;

    /**
     * 用户原始问题
     */
    private String question;

    /**
     * 最终意图
     */
    private String intent;

    /**
     * 意图识别置信度
     */
    private Double confidence;

    /**
     * Tool名称
     */
    private String toolName;

    /**
     * 是否执行Tool
     */
    private Boolean toolExecuted;

    /**
     * Tool请求参数JSON
     */
    private String toolRequest;

    /**
     * Tool响应结果JSON
     */
    private String toolResponse;

    /**
     * 关联工单ID
     */
    private Long ticketId;

    /**
     * 关联工单编号
     */
    private String ticketNo;

    /**
     * 执行状态：SUCCESS/PENDING_CONFIRM/UNSUPPORTED/FAILED
     */
    private String executionStatus;

    /**
     * 失败原因
     */
    private String errorMessage;

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