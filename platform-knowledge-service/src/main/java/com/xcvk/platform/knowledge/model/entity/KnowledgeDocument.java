package com.xcvk.platform.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xcvk.platform.common.enums.DeleteStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识文档表
 *
 * @author Programmer
 * @since 2026-04-24
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("kb_document")
public class KnowledgeDocument implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.NONE)
    private Long id;

    /**
     * 知识标题
     */
    private String title;

    /**
     * 知识摘要
     */
    private String summary;

    /**
     * 知识正文
     */
    private String content;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称快照
     */
    private String categoryName;

    /**
     * 标签，多个标签用英文逗号分隔
     */
    private String tags;

    /**
     * 状态：DRAFT/PUBLISHED/OFFLINE
     */
    private String status;

    /**
     * 创建人ID
     */
    private Long creatorId;

    /**
     * 创建人名称快照
     */
    private String creatorName;

    /**
     * 发布时间
     */
    private LocalDateTime publishedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 是否删除：0否，1是
     */
    private DeleteStatus deleted;
}