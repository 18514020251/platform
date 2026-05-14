package com.xcvk.platform.workflow.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xcvk.platform.workflow.entity.SearchSyncTask;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索索引同步任务 Mapper。
 */
@Mapper
public interface SearchSyncTaskMapper extends BaseMapper<SearchSyncTask> {

    /**
     * 新增或刷新同步任务。
     *
     * <p>如果同一个 dedupeKey 已经存在，则把任务重新置为 PENDING，
     * 表示以最新业务数据重新同步 ES。</p>
     */
    @Insert("""
            INSERT INTO search_sync_task (
                id, biz_type, biz_id, operation, dedupe_key,
                status, retry_count, max_retry, next_retry_at,
                last_error, created_at, updated_at
            )
            VALUES (
                #{id}, #{bizType}, #{bizId}, #{operation}, #{dedupeKey},
                #{status}, #{retryCount}, #{maxRetry}, #{nextRetryAt},
                #{lastError}, NOW(), NOW()
            )
            ON DUPLICATE KEY UPDATE
                status = 'PENDING',
                retry_count = 0,
                max_retry = VALUES(max_retry),
                next_retry_at = NOW(),
                last_error = NULL,
                updated_at = NOW()
            """)
    int upsertTask(SearchSyncTask task);

    /**
     * 查询到期任务。
     */
    @Select("""
            SELECT id, biz_type, biz_id, operation, dedupe_key,
                   status, retry_count, max_retry, next_retry_at,
                   last_error, locked_at, created_at, updated_at
            FROM search_sync_task
            WHERE status IN ('PENDING', 'RETRYING')
              AND next_retry_at <= NOW()
              AND retry_count < max_retry
            ORDER BY next_retry_at ASC, id ASC
            LIMIT #{limit}
            """)
    List<SearchSyncTask> selectDueTasks(@Param("limit") int limit);

    /**
     * 抢占任务。
     *
     * <p>即使后面扩成多实例部署，也可以避免同一任务被重复执行。</p>
     */
    @Update("""
            UPDATE search_sync_task
            SET status = 'RUNNING',
                locked_at = NOW(),
                updated_at = NOW()
            WHERE id = #{id}
              AND status IN ('PENDING', 'RETRYING')
              AND next_retry_at <= NOW()
            """)
    int markRunning(@Param("id") Long id);

    /**
     * 标记成功。
     */
    @Update("""
            UPDATE search_sync_task
            SET status = 'SUCCESS',
                last_error = NULL,
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int markSuccess(@Param("id") Long id);

    /**
     * 标记失败或重试。
     */
    @Update("""
            UPDATE search_sync_task
            SET status = #{status},
                retry_count = #{retryCount},
                next_retry_at = #{nextRetryAt},
                last_error = #{lastError},
                updated_at = NOW()
            WHERE id = #{id}
            """)
    int markFailedOrRetrying(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("retryCount") int retryCount,
            @Param("nextRetryAt") LocalDateTime nextRetryAt,
            @Param("lastError") String lastError
    );

    /**
     * 恢复卡死的 RUNNING 任务。
     */
    @Update("""
            UPDATE search_sync_task
            SET status = 'RETRYING',
                next_retry_at = NOW(),
                updated_at = NOW()
            WHERE status = 'RUNNING'
              AND locked_at < DATE_SUB(NOW(), INTERVAL #{minutes} MINUTE)
            """)
    int recoverStaleRunningTasks(@Param("minutes") int minutes);
}