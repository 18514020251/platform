package com.xcvk.platform.common.config;

import com.xcvk.platform.common.exception.GlobalExceptionHandler;
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
}
