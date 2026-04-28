package com.xcvk.platform.ai.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DashScope Embedding 配置属性
 *
 * <p>用于配置阿里云 DashScope 文本向量模型。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-27
 */
@Data
@ConfigurationProperties(prefix = "platform.ai.embedding.dashscope")
public class DashScopeEmbeddingProperties {

    /**
     * DashScope API Key
     */
    private String apiKey;

    /**
     * Embedding 模型名称
     */
    private String modelName = "text-embedding-v3";
}