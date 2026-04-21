package com.xcvk.platform.auth.starter.constant;

/**
 * 平台角色常量
 *
 * <p>当前阶段用于统一各业务服务中的角色编码语义，
 * 避免不同模块分别维护重复的角色常量，导致命名漂移或判断不一致。</p>
 *
 * <p>由于这些角色常量本质上服务于认证、授权和登录身份相关能力，
 * 因此暂时放在 satoken starter 中统一复用，
 * 而不是下沉到 common 模块，避免 common 承担过多业务语义。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-21
 */
public final class PlatformRoleConstants {

    private PlatformRoleConstants() {
    }

    /**
     * 系统管理员
     */
    public static final String ADMIN = "ADMIN";

    /**
     * 普通员工
     */
    public static final String EMPLOYEE = "EMPLOYEE";

    /**
     * 支持人员
     */
    public static final String SUPPORT = "SUPPORT";

    /**
     * 知识库管理员
     */
    public static final String KNOWLEDGE_ADMIN = "KNOWLEDGE_ADMIN";
}