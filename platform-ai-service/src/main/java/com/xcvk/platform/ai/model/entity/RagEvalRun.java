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
 * RAG 离线评测任务汇总结果。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("rag_eval_run")
public class RagEvalRun implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    private Integer datasetSize;

    private Integer retrieveTopK;

    private Long categoryId;

    private Boolean generationEnabled;

    private Boolean judgeEnabled;

    private Double recallAt1;

    private Double recallAt3;

    private Double recallAt5;

    private Double mrr;

    private Double contextPrecisionAt5;

    private Double avgFaithfulnessScore;

    private Double avgRelevanceScore;

    private Double avgRetrieveLatencyMs;

    private Double avgAnswerLatencyMs;

    private String retrievalMode;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}