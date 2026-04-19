package com.xcvk.platform.redis.starter.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redis 启动检查配置
 *
 * <p>当前只控制是否启用启动检查，以及失败时是否阻断应用启动。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-19
 */
@ConfigurationProperties(prefix = "platform.redis.health-check")
public class PlatformRedisHealthCheckProperties {

    /**
     * 是否启用 Redis 启动检查
     */
    private boolean enabled = true;

    /**
     * 检查失败时是否阻断应用启动
     */
    private boolean failFast = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isFailFast() {
        return failFast;
    }

    public void setFailFast(boolean failFast) {
        this.failFast = failFast;
    }
}