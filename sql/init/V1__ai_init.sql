CREATE DATABASE IF NOT EXISTS platform_ai
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE platform_ai;

CREATE TABLE IF NOT EXISTS ai_agent_execution_log
(
    id               BIGINT       NOT NULL COMMENT '主键ID',
    user_id          BIGINT       NOT NULL COMMENT '用户ID',
    username         VARCHAR(64)  NOT NULL COMMENT '用户名称快照',
    question         VARCHAR(1000) NOT NULL COMMENT '用户原始问题',

    intent           VARCHAR(64)  NOT NULL COMMENT '最终意图',
    confidence       DOUBLE       NULL COMMENT '意图识别置信度',

    tool_name        VARCHAR(64)  NULL COMMENT 'Tool名称',
    tool_executed    TINYINT      NOT NULL DEFAULT 0 COMMENT '是否执行Tool：0否，1是',
    tool_request     TEXT         NULL COMMENT 'Tool请求参数JSON',
    tool_response    TEXT         NULL COMMENT 'Tool响应结果JSON',

    ticket_id        BIGINT       NULL COMMENT '关联工单ID',
    ticket_no        VARCHAR(64)  NULL COMMENT '关联工单编号',

    execution_status VARCHAR(32)  NOT NULL COMMENT '执行状态：SUCCESS/PENDING_CONFIRM/UNSUPPORTED/FAILED',
    error_message    VARCHAR(1000) NULL COMMENT '失败原因',

    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_user_created_at (user_id, created_at),
    KEY idx_intent_created_at (intent, created_at),
    KEY idx_ticket_no (ticket_no),
    KEY idx_execution_status (execution_status)
    ) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci
    COMMENT = 'AI Agent执行日志表';