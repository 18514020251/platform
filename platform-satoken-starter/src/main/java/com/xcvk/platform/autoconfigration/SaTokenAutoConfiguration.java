package com.xcvk.platform.autoconfigration;

import cn.dev33.satoken.config.SaTokenConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * sa-token 配置
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18 10:40
 */
@AutoConfiguration
public class SaTokenAutoConfiguration {

    private static final long TOKEN_TIMEOUT_ONE_DAY = 60L * 60 * 24;

    @Bean
    public SaTokenConfig saTokenConfig() {
        SaTokenConfig config = new SaTokenConfig();
        config.setTokenName("Authorization");
        config.setTimeout(TOKEN_TIMEOUT_ONE_DAY);
        config.setIsConcurrent(true);
        config.setIsShare(true);
        config.setTokenStyle("uuid");
        return config;
    }
}
