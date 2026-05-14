package com.xcvk.platform.workflow.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 内部服务 Feign 调用配置。
 *
 * <p>workflow-service 调用 auth-service 的 /internal/** 接口时，
 * 自动携带内部访问令牌。</p>
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