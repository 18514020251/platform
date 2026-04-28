package com.xcvk.platform.ai.service;

import com.xcvk.platform.ai.model.dto.EmbeddingRequest;
import com.xcvk.platform.ai.model.vo.EmbeddingResponse;

/**
 * 文本向量化服务
 *
 * <p>用于将文本转换为 embedding 向量。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-27
 */
public interface EmbeddingService {

    /**
     * 批量生成文本向量。
     *
     * @param request 文本向量化请求
     * @return 文本向量化响应
     */
    EmbeddingResponse embedTexts(EmbeddingRequest request);
}