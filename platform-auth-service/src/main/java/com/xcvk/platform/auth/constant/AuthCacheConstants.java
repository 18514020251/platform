package com.xcvk.platform.auth.constant;

import java.time.Duration;

/**
 * 认证缓存常量
 *
 * <p>当前阶段只缓存登录用户身份上下文，
 * 统一管理 Redis key 前缀和过期时间，避免散落在业务代码中。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-19
 */
public final class AuthCacheConstants {

    private AuthCacheConstants() {
    }

    /**
     * 登录用户身份缓存前缀
     */
    public static final String LOGIN_USER_KEY_PREFIX = "auth:login-user:";

    /**
     * 登录用户身份缓存过期时间
     */
    public static final Duration LOGIN_USER_TTL = Duration.ofMinutes(30);

    public static String buildLoginUserKey(Long userId) {
        return LOGIN_USER_KEY_PREFIX + userId;
    }
}