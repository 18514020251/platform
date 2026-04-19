package com.xcvk.platform.auth.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcvk.platform.auth.constant.AuthCacheConstants;
import com.xcvk.platform.auth.model.security.LoginUser;
import com.xcvk.platform.auth.service.AuthCacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 认证缓存服务实现类
 *
 * <p>当前仅缓存 LoginUser，
 * 目的是把登录用户身份上下文沉淀到 Redis，为后续鉴权相关能力做准备。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-19
 */
@Service
@RequiredArgsConstructor
public class AuthCacheServiceImpl implements AuthCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public LoginUser getLoginUser(Long userId) {
        try {
            String jsonValue = stringRedisTemplate.opsForValue().get(
                    AuthCacheConstants.buildLoginUserKey(userId)
            );

            if (jsonValue == null) {
                return null;
            }

            return objectMapper.readValue(jsonValue, LoginUser.class);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void cacheLoginUser(LoginUser loginUser) {
        try {
            String jsonValue = objectMapper.writeValueAsString(loginUser);
            stringRedisTemplate.opsForValue().set(
                    AuthCacheConstants.buildLoginUserKey(loginUser.userId()),
                    jsonValue,
                    AuthCacheConstants.LOGIN_USER_TTL
            );
        } catch (Exception e) {
            throw new RuntimeException("Cache login user failed", e);
        }
    }

    @Override
    public void evictLoginUser(Long userId) {
        stringRedisTemplate.delete(AuthCacheConstants.buildLoginUserKey(userId));
    }
}