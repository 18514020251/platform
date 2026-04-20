package com.xcvk.platform.auth.starter.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token WebMvc 配置
 *
 * <p>注册 Sa-Token 拦截器，打开注解式鉴权功能。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
@AutoConfiguration
@ConditionalOnClass(SaInterceptor.class)
public class SaTokenWebMvcConfiguration implements WebMvcConfigurer {

    /**
     * 注册 Sa-Token 拦截器，开启 @SaCheckLogin / @SaCheckRole / @SaCheckPermission 等注解鉴权。
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
    }
}