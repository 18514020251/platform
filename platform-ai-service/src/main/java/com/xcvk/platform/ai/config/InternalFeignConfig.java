package com.xcvk.platform.ai.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InternalFeignConfig {

    @Bean
    public RequestInterceptor internalTokenRequestInterceptor(
            @Value("${platform.internal.token}") String internalToken
    ) {
        return template -> template.header("X-Internal-Token", internalToken);
    }
}