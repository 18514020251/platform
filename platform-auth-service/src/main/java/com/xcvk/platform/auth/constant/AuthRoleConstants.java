package com.xcvk.platform.auth.constant;

/**
 * 认证角色常量
 *
 * <p>当前阶段先在 auth 模块内维护角色编码常量，
 * 待多个业务服务稳定复用后，再提升到共享契约模块。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public final class AuthRoleConstants {

    private AuthRoleConstants() {
    }
    /* 管理员 */
    public static final String ADMIN = "ADMIN";
    /* 员工 */
    public static final String EMPLOYEE = "EMPLOYEE";
    /* 支持者 */
    public static final String SUPPORT = "SUPPORT";
    /* 知识库管理员 */
    public static final String KNOWLEDGE_ADMIN = "KNOWLEDGE_ADMIN";
}