package com.xcvk.platform.redis.starter.config;

import com.xcvk.platform.redis.starter.properties.PlatformRedisHealthCheckProperties;
import com.xcvk.platform.redis.starter.startup.RedisConnectionChecker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Redis 启动检查自动配置
 *
 * <p>该检查的目的不是代替运维监控，而是在本地开发和服务启动阶段
 * 尽早暴露 Redis 配置错误或连接不可用的问题。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-19
 */
@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@EnableConfigurationProperties(PlatformRedisHealthCheckProperties.class)
@ConditionalOnProperty(
        prefix = "platform.redis.health-check",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PlatformRedisHealthCheckAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(RedisConnectionChecker.class)
    public RedisConnectionChecker redisConnectionChecker(StringRedisTemplate stringRedisTemplate,
                                                         RedisProperties redisProperties,
                                                         PlatformRedisHealthCheckProperties properties) {
        return new RedisConnectionChecker(stringRedisTemplate, redisProperties, properties);
    }
}