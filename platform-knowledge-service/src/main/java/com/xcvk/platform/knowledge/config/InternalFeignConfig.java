package com.xcvk.platform.knowledge.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 内部服务 Feign 调用配置。
 *
 * <p>用于 knowledge-service 调用 ai-service 的 /internal/** 接口时，
 * 自动携带内部访问令牌，避免 embedding 接口被外部直接调用。</p>
 *
 * @author Programmer
 * @version 1.0
 */
@Configuration
public class InternalFeignConfig {

    @Bean
    public RequestInterceptor internalTokenRequestInterceptor(
            @Value("${platform.internal.token}") String internalToken
    ) {
        return template -> template.header("X-Internal-Token", internalToken);
    }
}