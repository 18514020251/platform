-- =========================
-- workflow 最小表结构（支持重复执行）
-- =========================

CREATE DATABASE IF NOT EXISTS platform_workflow;
USE platform_workflow;

DROP TABLE IF EXISTS wf_ticket;
DROP TABLE IF EXISTS wf_ticket_type;

CREATE TABLE IF NOT EXISTS wf_ticket_type (
    id                    BIGINT PRIMARY KEY COMMENT '工单类型ID',
    type_code             VARCHAR(64)  NOT NULL COMMENT '工单类型编码',
    type_name             VARCHAR(64)  NOT NULL COMMENT '工单类型名称',
    status                TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用，0禁用',
    description           VARCHAR(255) DEFAULT NULL COMMENT '类型说明',
    default_priority      VARCHAR(32)  DEFAULT NULL COMMENT '默认优先级',
    default_assignee_role VARCHAR(64)  DEFAULT NULL COMMENT '默认处理角色编码',
    allow_ai_create       TINYINT      NOT NULL DEFAULT 1 COMMENT '是否允许AI创建：1是，0否',
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_wf_ticket_type_code (type_code),
    KEY idx_wf_ticket_type_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单类型表';

CREATE TABLE IF NOT EXISTS wf_ticket (
    id                BIGINT PRIMARY KEY COMMENT '工单ID',
    ticket_no         VARCHAR(32)  NOT NULL COMMENT '工单编号',
    ticket_type_id    BIGINT       NOT NULL COMMENT '工单类型ID',
    ticket_type_code  VARCHAR(64)  NOT NULL COMMENT '工单类型编码快照',
    ticket_type_name  VARCHAR(64)  NOT NULL COMMENT '工单类型名称快照',
    title             VARCHAR(128) NOT NULL COMMENT '工单标题',
    content           TEXT         NOT NULL COMMENT '工单内容',
    status            VARCHAR(32)  NOT NULL COMMENT '当前状态',
    priority          VARCHAR(32)  NOT NULL COMMENT '优先级',
    source            VARCHAR(32)  NOT NULL COMMENT '工单来源：MANUAL/AI_AGENT',
    source_ref        VARCHAR(64)  DEFAULT NULL COMMENT '来源关联ID，如AI会话ID',
    creator_id        BIGINT       NOT NULL COMMENT '创建人ID',
    creator_name      VARCHAR(64)  NOT NULL COMMENT '创建人名称快照',
    assignee_id       BIGINT       DEFAULT NULL COMMENT '当前处理人ID',
    assignee_name     VARCHAR(64)  DEFAULT NULL COMMENT '当前处理人名称快照',
    closed_at         DATETIME     DEFAULT NULL COMMENT '关闭时间',
    status_remark     VARCHAR(500) DEFAULT NULL comment '当前状态说明/处理备注',
    created_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at        DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_wf_ticket_ticket_no (ticket_no),
    KEY idx_wf_ticket_ticket_type_id (ticket_type_id),
    KEY idx_wf_ticket_creator_id (creator_id),
    KEY idx_wf_ticket_assignee_id (assignee_id),
    KEY idx_wf_ticket_status (status),
    KEY idx_wf_ticket_source (source),
    KEY idx_wf_ticket_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单主表';
