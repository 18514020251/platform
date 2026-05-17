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
 * RAG 离线评测数据集。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("rag_eval_dataset")
public class RagEvalDataset implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.INPUT)
    private Long id;

    /**
     *   问题
     * */
    private String question;

    /**
     *   预期答案的片段ID
     * */
    private String expectedChunkIds;

    /**
     *   预期答案
     * */
    private String expectedAnswer;

    /**
     *   问题类别ID
     * */
    private Long categoryId;

    /**
     *   问题难度
     * */
    private String difficulty;

    /**
     *   问题创建时间
     * */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     *   问题更新时间
     * */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}