package com.xcvk.platform.workflow.scheduler;

import com.xcvk.platform.workflow.constant.SearchSyncTaskConstants;
import com.xcvk.platform.workflow.entity.SearchSyncTask;
import com.xcvk.platform.workflow.repository.mapper.SearchSyncTaskMapper;
import com.xcvk.platform.workflow.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索索引同步任务处理器。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SearchSyncTaskProcessor {

    private static final int STALE_RUNNING_MINUTES = 10;

    private final SearchSyncTaskMapper searchSyncTaskMapper;

    private final TicketService ticketService;

    @Value("${platform.search-sync.batch-size:20}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${platform.search-sync.fixed-delay-ms:5000}")
    public void processTasks() {
        searchSyncTaskMapper.recoverStaleRunningTasks(STALE_RUNNING_MINUTES);

        List<SearchSyncTask> tasks = searchSyncTaskMapper.selectDueTasks(batchSize);
        if (tasks == null || tasks.isEmpty()) {
            return;
        }

        for (SearchSyncTask task : tasks) {
            processOne(task);
        }
    }

    private void processOne(SearchSyncTask task) {
        int locked = searchSyncTaskMapper.markRunning(task.getId());
        if (locked != 1) {
            return;
        }

        try {
            dispatch(task);
            searchSyncTaskMapper.markSuccess(task.getId());
        } catch (Exception ex) {
            markFailure(task, ex);
        }
    }

    private void dispatch(SearchSyncTask task) {
        if (SearchSyncTaskConstants.BIZ_TYPE_TICKET.equals(task.getBizType())
                && SearchSyncTaskConstants.OPERATION_UPSERT.equals(task.getOperation())) {
            ticketService.syncTicketToSearchIndex(task.getBizId());
            return;
        }

        throw new IllegalArgumentException(
                "不支持的搜索同步任务：bizType=" + task.getBizType()
                        + ", operation=" + task.getOperation()
        );
    }

    private void markFailure(SearchSyncTask task, Exception ex) {
        int nextRetryCount = task.getRetryCount() + 1;

        String errorMessage = buildErrorMessage(ex);

        if (nextRetryCount >= task.getMaxRetry()) {
            searchSyncTaskMapper.markFailedOrRetrying(
                    task.getId(),
                    SearchSyncTaskConstants.STATUS_FAILED,
                    nextRetryCount,
                    LocalDateTime.now(),
                    errorMessage
            );

            log.error(
                    "搜索索引同步任务最终失败，taskId={}, bizType={}, bizId={}, operation={}, retryCount={}",
                    task.getId(),
                    task.getBizType(),
                    task.getBizId(),
                    task.getOperation(),
                    nextRetryCount,
                    ex
            );
            return;
        }

        LocalDateTime nextRetryAt = LocalDateTime.now().plusSeconds(calculateBackoffSeconds(nextRetryCount));

        searchSyncTaskMapper.markFailedOrRetrying(
                task.getId(),
                SearchSyncTaskConstants.STATUS_RETRYING,
                nextRetryCount,
                nextRetryAt,
                errorMessage
        );

        log.warn(
                "搜索索引同步任务失败，将自动重试，taskId={}, bizType={}, bizId={}, operation={}, retryCount={}, nextRetryAt={}, error={}",
                task.getId(),
                task.getBizType(),
                task.getBizId(),
                task.getOperation(),
                nextRetryCount,
                nextRetryAt,
                errorMessage
        );
    }

    /**
     * 指数退避：5s、10s、20s、40s、80s、160s，最大 300s。
     */
    private long calculateBackoffSeconds(int retryCount) {
        long seconds = 5L * (1L << Math.min(retryCount - 1, 6));
        return Math.min(seconds, 300L);
    }

    private String buildErrorMessage(Exception ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }

        if (message.length() > 1000) {
            return message.substring(0, 1000);
        }

        return message;
    }
}