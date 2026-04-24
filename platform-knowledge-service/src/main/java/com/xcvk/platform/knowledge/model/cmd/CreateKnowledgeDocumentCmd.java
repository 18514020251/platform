package com.xcvk.platform.knowledge.model.cmd;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建知识文档命令
 *
 * <p>用于承接创建知识文档的业务入参。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-24
 */
public record CreateKnowledgeDocumentCmd(

        String title,

        String summary,

        String content,

        Long categoryId,

        String categoryName,

        String tags,

        Long creatorId,

        String creatorName

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}