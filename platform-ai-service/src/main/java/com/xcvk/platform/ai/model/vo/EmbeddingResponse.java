package com.xcvk.platform.ai.model.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 文本向量化响应
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-27
 */
public record EmbeddingResponse(

        /**
         * 模型名称
         */
        String modelName,

        /**
         * 向量维度
         */
        Integer dimension,

        /**
         * 向量列表
         */
        List<List<Float>> vectors

) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}