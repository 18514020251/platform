package com.xcvk.platform.auth.starter.config;

import com.xcvk.platform.auth.starter.util.SaTokenUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * sa-token 配置类
 *
 * <p>该配置类提供了 sa-token 的自动装配。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18
 */
@Configuration
public class SaTokenAutoConfiguration {

    @Bean
    public SaTokenUtils saTokenUtils() {
        return new SaTokenUtils();
    }

}