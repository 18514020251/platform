package com.xcvk.platform.workflow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 搜索索引同步任务。
 */
@Data
@TableName("search_sync_task")
public class SearchSyncTask {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String bizType;

    private Long bizId;

    private String operation;

    private String dedupeKey;

    private String status;

    private Integer retryCount;

    private Integer maxRetry;

    private LocalDateTime nextRetryAt;

    private String lastError;

    private LocalDateTime lockedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}