package com.xcvk.platform.api.contract.ai.model;

import jakarta.validation.constraints.NotEmpty;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 文本向量化请求
 *
 * <p>用于接收需要生成 embedding 的文本列表。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-27
 */
public record EmbeddingRequest(

        @NotEmpty(message = "向量化文本不能为空")
        List<String> texts

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}