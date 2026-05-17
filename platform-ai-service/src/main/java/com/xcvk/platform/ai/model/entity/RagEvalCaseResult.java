package com.xcvk.platform.ai.model.entity;

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
 * RAG 离线评测单样本结果。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("rag_eval_case_result")
public class RagEvalCaseResult implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private Long runId;

    private Long datasetId;

    private String question;

    private String expectedChunkIds;

    private String retrievedChunkIds;

    private Boolean hitAt1;

    private Boolean hitAt3;

    private Boolean hitAt5;

    private Double reciprocalRank;

    private Double contextPrecisionAt5;

    private String answer;

    private Double faithfulnessScore;

    private Double relevanceScore;

    private Long retrieveLatencyMs;

    private Long answerLatencyMs;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}