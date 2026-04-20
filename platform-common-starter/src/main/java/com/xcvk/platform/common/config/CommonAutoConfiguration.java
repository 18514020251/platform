package com.xcvk.platform.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.xcvk.platform.common.exception.GlobalExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 全局异常处理器配置文件
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18 11:26
 */
@Configuration
public class CommonAutoConfiguration {

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Configuration
    @ConditionalOnClass(MetaObjectHandler.class)
    @ConditionalOnProperty(
            prefix = "platform.mybatis-plus",
            name = "auto-fill-enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public static class MybatisPlusAutoConfiguration {

        private static final Logger log = LoggerFactory.getLogger(MybatisPlusAutoConfiguration.class);

        @Bean
        @ConditionalOnMissingBean
        public MetaObjectHandler metaObjectHandler() {
            log.info("初始化 MyBatis-Plus 自动填充处理器");
            return new MybatisPlusMetaObjectHandler();
        }
    }
}
