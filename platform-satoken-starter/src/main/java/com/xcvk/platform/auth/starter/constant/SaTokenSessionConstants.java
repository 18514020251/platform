package com.xcvk.platform.auth.starter.constant;

/**
 * Sa-Token Session 常量
 *
 * <p>统一管理写入 Sa-Token Session 的字段名，避免魔法值散落在业务代码中。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public final class SaTokenSessionConstants {

    private SaTokenSessionConstants() {
    }

    /**
     * 当前登录用户名
     */
    public static final String USERNAME = "username";

    /**
     * 当前登录用户真实姓名
     */
    public static final String REAL_NAME = "realName";

    /**
     * 当前登录用户角色编码列表
     */
    public static final String ROLE_CODES = "roleCodes";
}