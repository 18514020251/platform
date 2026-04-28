package com.xcvk.platform.ai.config;

import com.xcvk.platform.ai.properties.DashScopeEmbeddingProperties;
import dev.langchain4j.community.model.dashscope.QwenEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * AI Embedding 配置
 *
 * <p>当前阶段使用 DashScope text-embedding-v3 作为文本向量模型，
 * 对外统一暴露 LangChain4j 的 EmbeddingModel 接口。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-27
 */
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(DashScopeEmbeddingProperties.class)
@Slf4j
public class AiEmbeddingConfiguration {

    private final DashScopeEmbeddingProperties dashScopeEmbeddingProperties;

    /**
     * DashScope 文本向量模型。
     *
     * @return LangChain4j EmbeddingModel
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        if (!StringUtils.hasText(dashScopeEmbeddingProperties.getApiKey())) {
            throw new IllegalStateException("DashScope API Key 不能为空，请配置 platform.ai.embedding.dashscope.api-key");
        }

        log.info("""
        ================================================
        DashScope Embedding 配置
        API Key  :  configured
        Model    :  {}
        ================================================
        """,
                dashScopeEmbeddingProperties.getModelName()
        );
        return QwenEmbeddingModel.builder()
                .apiKey(dashScopeEmbeddingProperties.getApiKey())
                .modelName(dashScopeEmbeddingProperties.getModelName())
                .build();
    }
}