package com.xcvk.platform.ai.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 知识缺口转工单去重记录。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("ai_knowledge_gap_ticket")
public class AiKnowledgeGapTicket implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     * 用户原始问题。
     */
    private String rawQuestion;

    /**
     * 原始问题 Hash。
     */
    private String rawQuestionHash;

    /**
     * LLM 规范化后的问题。
     */
    private String normalizedQuestion;

    /**
     * 规范化问题 Hash。
     */
    private String normalizedQuestionHash;

    /**
     * 关联工单 ID。
     */
    private Long ticketId;

    /**
     * 关联工单编号。
     */
    private String ticketNo;

    /**
     * 工单状态快照。
     */
    private String ticketStatus;

    /**
     * 工单类型编码。
     */
    private String ticketTypeCode;

    /**
     * 工单标题。
     */
    private String ticketTitle;

    /**
     * 首次触发用户 ID。
     */
    private Long createdBy;

    /**
     * 重复命中次数。
     */
    private Integer hitCount;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}