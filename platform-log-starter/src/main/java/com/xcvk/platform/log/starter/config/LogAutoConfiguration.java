package com.xcvk.platform.log.starter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcvk.platform.log.starter.aspect.AccessLogAspect;
import com.xcvk.platform.log.starter.properties.AccessLogProperties;
import com.xcvk.platform.log.starter.support.AccessLogSanitizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 访问日志自动装配
 *
 * <p>只在 Servlet Web 环境下启用访问日志切面，
 * 并通过配置开关控制是否生效。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-19
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(AccessLogProperties.class)
@ConditionalOnProperty(prefix = "platform.access-log", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LogAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AccessLogSanitizer accessLogSanitizer(ObjectMapper objectMapper,
                                                 AccessLogProperties properties) {
        return new AccessLogSanitizer(objectMapper, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public AccessLogAspect accessLogAspect(ObjectMapper objectMapper,
                                           AccessLogSanitizer sanitizer,
                                           AccessLogProperties properties) {
        return new AccessLogAspect(objectMapper, sanitizer, properties);
    }
}