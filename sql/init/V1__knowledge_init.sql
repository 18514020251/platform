-- =========================
-- knowledge 最小表结构（支持重复执行）
-- =========================

CREATE DATABASE IF NOT EXISTS platform_knowledge;
USE platform_knowledge;

DROP TABLE IF EXISTS kb_document;
DROP TABLE IF EXISTS kb_document_chunk;

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


CREATE TABLE kb_document_chunk (
    id BIGINT PRIMARY KEY COMMENT '主键ID',

    document_id BIGINT NOT NULL COMMENT '知识文档ID',
    chunk_no INT NOT NULL COMMENT '切片序号，从1开始',

    chunk_text TEXT NOT NULL COMMENT '切片文本内容',
    chunk_hash VARCHAR(64) NOT NULL COMMENT '切片内容哈希',

    token_count INT NOT NULL DEFAULT 0 COMMENT 'token数量，当前阶段可用字符数近似',

    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/OFFLINE',

    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NOT NULL COMMENT '更新时间',

    UNIQUE KEY uk_document_chunk_no (document_id, chunk_no),
    KEY idx_chunk_document_id (document_id),
    KEY idx_chunk_status (status),
    KEY idx_chunk_document_status (document_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识文档切片表';