package com.xcvk.platform.log.starter.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 访问日志配置。
 *
 * <p>配置类需要被 Spring Boot 绑定，因此保留可变 class；日志记录、查询请求和返回对象使用 record。</p>
 *
 * @author Programmer
 * @version 1.1
 * @date 2026-05-11
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "platform.access-log")
public class AccessLogProperties {

    /**
     * 是否启用访问日志切面。
     */
    private boolean enabled = true;

    /**
     * 默认是否记录请求参数。
     */
    private boolean recordArgs = true;

    /**
     * 默认是否记录响应结果。
     */
    private boolean recordResult = false;

    /**
     * 单段日志内容最大长度，超过后截断。
     */
    private int maxContentLength = 2000;

    /**
     * 是否异步写入 MongoDB。
     */
    private boolean mongoEnabled = true;

    /**
     * MongoDB 访问日志集合名。
     */
    private String collectionName = "platform_access_log";

    /**
     * 启动时是否自动创建 MongoDB 索引。
     */
    private boolean createIndexes = true;

    /**
     * 访问日志保留天数，默认 30 天；小于等于 0 表示不创建 TTL 索引。
     */
    private long ttlDays = 30;

    /**
     * 异步写日志线程池核心线程数。
     */
    private int asyncCorePoolSize = 2;

    /**
     * 异步写日志线程池最大线程数。
     */
    private int asyncMaxPoolSize = 4;

    /**
     * 异步写日志队列容量。
     */
    private int asyncQueueCapacity = 1000;

    /**
     * 异步线程名前缀。
     */
    private String asyncThreadNamePrefix = "access-log-";

    /**
     * 停机时等待异步日志写入的最长秒数。
     */
    private int asyncAwaitTerminationSeconds = 3;

    /**
     * 需要脱敏的字段名。
     */
    private List<String> sensitiveFields = new ArrayList<>(List.of(
            "password",
            "token",
            "accessToken",
            "refreshToken",
            "authorization",
            "secret",
            "credential"
    ));
}
