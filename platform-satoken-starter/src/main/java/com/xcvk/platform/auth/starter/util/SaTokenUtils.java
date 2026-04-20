package com.xcvk.platform.auth.starter.util;

import cn.dev33.satoken.stp.StpUtil;

/**
 * 登录用户信息工具类
 *
 * <p>该工具类负责获取当前登录用户信息。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18
 */
public class SaTokenUtils {

    /**
     * 获取当前登录用户ID
     *
     * @return 当前登录用户ID
     */
    public Long getCurrentUserId() {
        return Long.valueOf(String.valueOf(StpUtil.getLoginId()));
    }
}