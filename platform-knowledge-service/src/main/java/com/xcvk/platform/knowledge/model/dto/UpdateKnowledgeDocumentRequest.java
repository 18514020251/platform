package com.xcvk.platform.knowledge.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新知识文档请求
 *
 * <p>用于接收前端更新知识文档的参数。</p>
 *
 * <p>注意：文档ID由路径参数传入，不放在请求体中；
 * 创建人、创建时间、发布时间、状态等系统字段也不允许通过该请求修改。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-25
 */
public record UpdateKnowledgeDocumentRequest(

        @NotBlank(message = "知识标题不能为空")
        @Size(max = 200, message = "知识标题长度不能超过200个字符")
        String title,

        @Size(max = 500, message = "知识摘要长度不能超过500个字符")
        String summary,

        @NotBlank(message = "知识正文不能为空")
        @Size(max = 10000, message = "知识正文长度不能超过10000个字符")
        String content,

        Long categoryId,

        @Size(max = 100, message = "分类名称长度不能超过100个字符")
        String categoryName,

        @Size(max = 500, message = "标签长度不能超过500个字符")
        String tags

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}