package com.xcvk.platform.auth.starter.config;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoRedisJackson;
import cn.dev33.satoken.stp.StpInterface;
import com.xcvk.platform.auth.starter.handler.SaTokenExceptionHandler;
import com.xcvk.platform.auth.starter.security.SessionStpInterface;
import com.xcvk.platform.auth.starter.startup.SaTokenDaoChecker;
import com.xcvk.platform.auth.starter.util.SaTokenSessionUtils;
import com.xcvk.platform.auth.starter.util.SaTokenUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Sa-Token 自动配置
 *
 * <p>当前阶段提供：</p>
 * <p>1. Redis 持久化 Dao</p>
 * <p>2. 当前登录人工具类</p>
 * <p>3. 基于 Session 的角色鉴权实现</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
@AutoConfiguration
@ConditionalOnClass(name = "cn.dev33.satoken.stp.StpUtil")
public class SaTokenAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SaTokenUtils saTokenUtils() {
        return new SaTokenUtils();
    }

    @Bean
    @ConditionalOnMissingBean
    public SaTokenSessionUtils saTokenSessionUtils() {
        return new SaTokenSessionUtils();
    }

    @Bean
    @ConditionalOnMissingBean(SaTokenDao.class)
    public SaTokenDao saTokenDao() {
        return new SaTokenDaoRedisJackson();
    }

    @Bean
    @ConditionalOnMissingBean(StpInterface.class)
    public StpInterface stpInterface(SaTokenSessionUtils saTokenSessionUtils) {
        return new SessionStpInterface(saTokenSessionUtils);
    }

    @Bean
    @ConditionalOnMissingBean
    public SaTokenDaoChecker saTokenDaoChecker() {
        return new SaTokenDaoChecker();
    }

    @Bean
    @ConditionalOnMissingBean
    public SaTokenExceptionHandler saTokenExceptionHandler() {
        return new SaTokenExceptionHandler();
    }
}