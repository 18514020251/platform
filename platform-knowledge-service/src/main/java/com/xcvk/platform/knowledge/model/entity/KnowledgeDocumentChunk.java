package com.xcvk.platform.knowledge.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;

import java.io.Serial;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableId;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 知识文档切片表
 * </p>
 *
 * @author Programmer
 * @since 2026-04-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("kb_document_chunk")
public class KnowledgeDocumentChunk implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.NONE)
    private Long id;

    /**
     * 知识文档ID
     */
    private Long documentId;

    /**
     * 切片序号，从1开始
     */
    private Integer chunkNo;

    /**
     * 切片文本内容
     */
    private String chunkText;

    /**
     * 切片内容哈希
     */
    private String chunkHash;

    /**
     * token数量，当前阶段可用字符数近似
     */
    private Integer tokenCount;

    /**
     * 状态：ACTIVE/OFFLINE
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;


}
