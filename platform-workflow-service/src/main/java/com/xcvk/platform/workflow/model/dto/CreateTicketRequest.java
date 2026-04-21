package com.xcvk.platform.workflow.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serial;
import java.io.Serializable;

/**
 * 创建工单请求
 *
 * <p>用于接收前端手工创建工单的参数。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public record CreateTicketRequest(

        @NotBlank(message = "工单类型编码不能为空")
        String ticketTypeCode,

        @NotBlank(message = "工单标题不能为空")
        @Size(max = 128, message = "工单标题长度不能超过128个字符")
        String title,

        @NotBlank(message = "工单内容不能为空")
        @Size(max = 2000, message = "工单内容长度不能超过2000个字符")
        String content,

        String priority

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}