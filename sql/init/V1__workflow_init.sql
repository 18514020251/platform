-- =========================
-- workflow 最小表结构（支持重复执行）
-- =========================

CREATE DATABASE IF NOT EXISTS platform_workflow;
USE platform_workflow;

DROP TABLE IF EXISTS wf_ticket_event;
DROP TABLE IF EXISTS wf_ticket;
DROP TABLE IF EXISTS wf_ticket_type;
DROP TABLE IF EXISTS search_sync_task;

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


CREATE TABLE IF NOT EXISTS wf_ticket_event (
    id              BIGINT PRIMARY KEY COMMENT '事件ID',
    ticket_id       BIGINT       NOT NULL COMMENT '工单ID',
    ticket_no       VARCHAR(32)  NOT NULL COMMENT '工单编号快照',
    event_type      VARCHAR(32)  NOT NULL COMMENT '事件类型：CREATE/ACCEPT/ASSIGN/RESOLVE/REJECT',
    operator_id     BIGINT       NOT NULL COMMENT '操作人ID',
    operator_name   VARCHAR(64)  NOT NULL COMMENT '操作人名称快照',
    from_status     VARCHAR(32)  DEFAULT NULL COMMENT '变更前状态',
    to_status       VARCHAR(32)  DEFAULT NULL COMMENT '变更后状态',
    remark          VARCHAR(500) DEFAULT NULL COMMENT '操作说明',
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    KEY idx_wf_ticket_event_ticket_id_created_at (ticket_id, created_at),
    KEY idx_wf_ticket_event_operator_id (operator_id),
    KEY idx_wf_ticket_event_event_type (event_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工单操作流水表';


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


CREATE TABLE IF NOT EXISTS search_sync_task (
    id BIGINT NOT NULL PRIMARY KEY COMMENT '主键ID',

    biz_type VARCHAR(32) NOT NULL COMMENT '业务类型，如 TICKET',
    biz_id BIGINT NOT NULL COMMENT '业务ID',
    operation VARCHAR(32) NOT NULL COMMENT '操作类型，如 UPSERT、DELETE',
    dedupe_key VARCHAR(128) NOT NULL COMMENT '去重键，如 TICKET:2001:UPSERT',

    status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING、RUNNING、RETRYING、SUCCESS、FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '已重试次数',
    max_retry INT NOT NULL DEFAULT 5 COMMENT '最大重试次数',
    next_retry_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次可重试时间',

    last_error VARCHAR(1000) NULL COMMENT '最近一次失败原因',
    locked_at DATETIME NULL COMMENT '任务开始执行时间',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_search_sync_dedupe (dedupe_key),
    KEY idx_search_sync_poll (status, next_retry_at, retry_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索索引同步任务表';