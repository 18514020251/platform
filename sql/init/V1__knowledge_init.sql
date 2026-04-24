-- =========================
-- knowledge 最小表结构（支持重复执行）
-- =========================

CREATE DATABASE IF NOT EXISTS platform_knowledge;
USE platform_knowledge;

DROP TABLE IF EXISTS kb_document;

CREATE TABLE kb_document (
    id BIGINT PRIMARY KEY COMMENT '主键ID',

    title VARCHAR(200) NOT NULL COMMENT '知识标题',
    summary VARCHAR(500) DEFAULT NULL COMMENT '知识摘要',
    content TEXT NOT NULL COMMENT '知识正文',

    category_id BIGINT DEFAULT NULL COMMENT '分类ID',
    category_name VARCHAR(100) DEFAULT NULL COMMENT '分类名称快照',

    tags VARCHAR(500) DEFAULT NULL COMMENT '标签，多个标签用英文逗号分隔',

    status VARCHAR(32) NOT NULL COMMENT '状态：DRAFT/PUBLISHED/OFFLINE',

    creator_id BIGINT DEFAULT NULL COMMENT '创建人ID',
    creator_name VARCHAR(100) DEFAULT NULL COMMENT '创建人名称快照',

    published_at DATETIME DEFAULT NULL COMMENT '发布时间',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',

    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除：0否，1是'
) COMMENT='知识文档表';