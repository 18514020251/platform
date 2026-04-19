package com.xcvk.platform.log.starter.annotation;

import java.lang.annotation.*;

/**
 * 访问日志注解
 *
 * <p>用于标记需要记录访问日志的接口方法。</p>
 * <p>第一版仅记录基础访问信息、请求参数、异常信息和耗时，
 * 默认不记录完整响应结果，避免把 token、敏感业务数据直接打入日志。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-19
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AccessLog {

    /**
     * 操作描述
     */
    String value() default "";

    /**
     * 是否记录请求参数
     */
    boolean recordArgs() default true;

    /**
     * 是否记录响应结果
     *
     * <p>默认关闭，避免敏感数据直接输出到日志。</p>
     */
    boolean recordResult() default false;
}