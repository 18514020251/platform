package com.xcvk.platform.ai.config;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 聊天模型配置
 *
 * <p>用于注册 LangChain4j ChatModel Bean，
 * 供 RAG 问答服务调用大模型生成回答。</p>
 *
 * <p>当前阶段使用 DeepSeek 的 OpenAI 兼容接口。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
@Configuration
public class AiChatConfiguration {

    private static final String DEEPSEEK_API_KEY_ENV = "DEEPSEEK_KEY";

    private static final String DEEPSEEK_BASE_URL = "https://api.deepseek.com";

    /**
     * 当前先使用兼容模型名。
     * 后续可按 DeepSeek /models 接口返回结果改为 deepseek-v4-flash 等模型名。
     */
    private static final String DEEPSEEK_MODEL_NAME = "deepseek-chat";

    /**
     * 注册 DeepSeek 聊天模型。
     *
     * @return ChatModel
     */
    @Bean
    public ChatModel chatModel() {
        String apiKey = System.getenv(DEEPSEEK_API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(DEEPSEEK_API_KEY_ENV + " environment variable is not set");
        }

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(DEEPSEEK_BASE_URL)
                .modelName(DEEPSEEK_MODEL_NAME)
                .temperature(0.7)
                .maxTokens(2000)
                .build();
    }
}