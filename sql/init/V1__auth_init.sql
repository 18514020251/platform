-- =========================
-- auth 最小表结构（支持重复执行）
-- =========================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS platform_auth;
USE platform_auth;

-- 删除表（按依赖关系倒序删除，避免外键约束）
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_dept;

-- =========================
-- 创建表结构
-- =========================

-- 部门表
CREATE TABLE IF NOT EXISTS sys_dept (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
    dept_name    VARCHAR(64) NOT NULL COMMENT '部门名称',
    status       TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_dept_name (dept_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统部门表';

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username     VARCHAR(64)  NOT NULL COMMENT '登录账号',
    password     VARCHAR(255) NOT NULL COMMENT '登录密码（加密后）',
    real_name    VARCHAR(64)  NOT NULL COMMENT '真实姓名',
    dept_id      BIGINT       DEFAULT NULL COMMENT '部门ID',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_dept_id (dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    role_code    VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name    VARCHAR(64) NOT NULL COMMENT '角色名称',
    status       TINYINT     NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_sys_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- 用户角色关系表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    user_id      BIGINT   NOT NULL COMMENT '用户ID',
    role_id      BIGINT   NOT NULL COMMENT '角色ID',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_sys_user_role_user_role (user_id, role_id),
    KEY idx_sys_user_role_user_id (user_id),
    KEY idx_sys_user_role_role_id (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关系表';