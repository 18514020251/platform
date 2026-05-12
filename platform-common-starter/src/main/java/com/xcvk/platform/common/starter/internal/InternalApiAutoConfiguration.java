package com.xcvk.platform.common.starter.internal;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@EnableConfigurationProperties(InternalApiProperties.class)
public class InternalApiAutoConfiguration implements WebMvcConfigurer {

    private final InternalApiProperties properties;

    public InternalApiAutoConfiguration(InternalApiProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new InternalApiInterceptor(properties))
                .addPathPatterns("/internal/**");
    }
}