package com.xcvk.platform.auth.service;

import com.xcvk.platform.auth.model.security.LoginUser;

/**
 * 认证缓存服务
 *
 * <p>主流程 service 不直接关心 Redis 细节，
 * 认证模块通过该服务统一完成登录用户身份上下文的查询、写入和删除。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-19
 */
public interface AuthCacheService {

    /**
     * 获取登录用户身份上下文
     *
     * @param userId 用户ID
     * @return 登录用户身份上下文，不存在时返回 null
     */
    LoginUser getLoginUser(Long userId);

    /**
     * 缓存登录用户身份上下文
     *
     * @param loginUser 登录用户身份上下文
     */
    void cacheLoginUser(LoginUser loginUser);

    /**
     * 删除登录用户身份上下文
     *
     * @param userId 用户ID
     */
    void evictLoginUser(Long userId);
}