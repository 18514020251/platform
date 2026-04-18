package com.xcvk.platform.id.autoconfigure;

import com.xcvk.platform.id.generator.SnowflakeIdGenerator;
import com.xcvk.platform.id.properties.IdProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * ID 自动配置类
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18
 */
@AutoConfiguration
@EnableConfigurationProperties(IdProperties.class)
public class IdAutoConfiguration {

    /**
     * 注册雪花ID生成器
     *
     * <p>默认基于 workerId 和 datacenterId 生成全局唯一ID，
     * 供业务模块直接注入使用。</p>
     *
     * @param properties ID 配置属性
     * @return 雪花ID生成器
     */
    @Bean
    @ConditionalOnMissingBean
    public SnowflakeIdGenerator snowflakeIdGenerator(IdProperties properties) {
        return new SnowflakeIdGenerator(properties.getWorkerId(), properties.getDatacenterId());
    }
}