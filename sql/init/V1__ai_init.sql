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

CREATE TABLE IF NOT EXISTS rag_eval_dataset
(
    id                 BIGINT        NOT NULL COMMENT '主键ID',
    question           VARCHAR(500)  NOT NULL COMMENT '评测问题',
    expected_chunk_ids VARCHAR(1000) NOT NULL COMMENT '标准相关chunk ID，逗号分隔',
    expected_answer    TEXT          NULL COMMENT '期望答案',
    category_id        BIGINT        NULL COMMENT '知识分类ID',
    difficulty         VARCHAR(32)   NOT NULL DEFAULT 'MEDIUM' COMMENT '难度：EASY/MEDIUM/HARD',
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    PRIMARY KEY (id),
    KEY idx_category_created_at (category_id, created_at),
    KEY idx_difficulty (difficulty)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = 'RAG离线评测数据集表';

CREATE TABLE IF NOT EXISTS rag_eval_run
(
    id                       BIGINT      NOT NULL COMMENT '主键ID',
    dataset_size             INT         NOT NULL COMMENT '本次评测样本数',
    retrieve_top_k           INT         NOT NULL DEFAULT 5 COMMENT '评测使用的召回topK',
    category_id              BIGINT      NULL COMMENT '分类ID过滤条件',
    generation_enabled       TINYINT     NOT NULL DEFAULT 0 COMMENT '是否执行生成答案：0否，1是',
    judge_enabled            TINYINT     NOT NULL DEFAULT 0 COMMENT '是否启用LLM-as-Judge：0否，1是',
    recall_at1               DOUBLE      NOT NULL DEFAULT 0 COMMENT 'Recall@1',
    recall_at3               DOUBLE      NOT NULL DEFAULT 0 COMMENT 'Recall@3',
    recall_at5               DOUBLE      NOT NULL DEFAULT 0 COMMENT 'Recall@5',
    mrr                      DOUBLE      NOT NULL DEFAULT 0 COMMENT 'MRR',
    context_precision_at5    DOUBLE      NOT NULL DEFAULT 0 COMMENT 'Context Precision@5',
    avg_faithfulness_score   DOUBLE      NULL COMMENT '平均Faithfulness分数',
    avg_relevance_score      DOUBLE      NULL COMMENT '平均Answer Relevance分数',
    avg_retrieve_latency_ms  DOUBLE      NOT NULL DEFAULT 0 COMMENT '平均召回耗时毫秒',
    avg_answer_latency_ms    DOUBLE      NOT NULL DEFAULT 0 COMMENT '平均回答耗时毫秒',
    created_at               DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (id),
    KEY idx_created_at (created_at),
    KEY idx_category_created_at (category_id, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = 'RAG离线评测任务表';

CREATE TABLE IF NOT EXISTS rag_eval_case_result
(
    id                       BIGINT        NOT NULL COMMENT '主键ID',
    run_id                   BIGINT        NOT NULL COMMENT '评测任务ID',
    dataset_id               BIGINT        NOT NULL COMMENT '数据集样本ID',
    question                 VARCHAR(500)  NOT NULL COMMENT '问题快照',
    expected_chunk_ids       VARCHAR(1000) NOT NULL COMMENT '标准相关chunk ID，逗号分隔',
    retrieved_chunk_ids      VARCHAR(1000) NULL COMMENT '实际召回chunk ID，逗号分隔',
    hit_at1                  TINYINT       NOT NULL DEFAULT 0 COMMENT 'Hit@1',
    hit_at3                  TINYINT       NOT NULL DEFAULT 0 COMMENT 'Hit@3',
    hit_at5                  TINYINT       NOT NULL DEFAULT 0 COMMENT 'Hit@5',
    reciprocal_rank          DOUBLE        NOT NULL DEFAULT 0 COMMENT '第一个相关chunk的倒数排名',
    context_precision_at5    DOUBLE        NOT NULL DEFAULT 0 COMMENT 'Context Precision@5',
    answer                   TEXT          NULL COMMENT '生成答案',
    faithfulness_score       DOUBLE        NULL COMMENT 'Faithfulness分数',
    relevance_score          DOUBLE        NULL COMMENT 'Answer Relevance分数',
    retrieve_latency_ms      BIGINT        NOT NULL DEFAULT 0 COMMENT '召回耗时毫秒',
    answer_latency_ms        BIGINT        NOT NULL DEFAULT 0 COMMENT '回答耗时毫秒',
    created_at               DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    PRIMARY KEY (id),
    KEY idx_run_id (run_id),
    KEY idx_dataset_id (dataset_id),
    KEY idx_hit_at5 (hit_at5)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
    COMMENT = 'RAG离线评测样本结果表';

ALTER TABLE rag_eval_run
    ADD COLUMN retrieval_mode varchar(32) DEFAULT 'ENHANCED_RRF' COMMENT '检索策略';