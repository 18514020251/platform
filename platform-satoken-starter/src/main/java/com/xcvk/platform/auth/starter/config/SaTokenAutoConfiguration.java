package com.xcvk.platform.auth.starter.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoRedisJackson;
import com.xcvk.platform.auth.starter.startup.SaTokenDaoChecker;
import com.xcvk.platform.auth.starter.util.SaTokenUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(name = "cn.dev33.satoken.stp.StpUtil")
public class SaTokenAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SaTokenUtils saTokenUtils() {
        return new SaTokenUtils();
    }

    @Bean
    @ConditionalOnMissingBean(SaTokenDao.class)
    public SaTokenDao saTokenDao() {
        return new SaTokenDaoRedisJackson();
    }

    @Bean
    @ConditionalOnMissingBean
    public SaTokenDaoChecker saTokenDaoChecker() {
        return new SaTokenDaoChecker();
    }
}