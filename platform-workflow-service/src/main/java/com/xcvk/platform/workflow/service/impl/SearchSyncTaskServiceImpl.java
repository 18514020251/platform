package com.xcvk.platform.workflow.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.workflow.constant.SearchSyncTaskConstants;
import com.xcvk.platform.workflow.entity.SearchSyncTask;
import com.xcvk.platform.workflow.repository.mapper.SearchSyncTaskMapper;
import com.xcvk.platform.workflow.service.SearchSyncTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 搜索索引同步任务服务实现。
 */
@Service
@RequiredArgsConstructor
public class SearchSyncTaskServiceImpl implements SearchSyncTaskService {

    private static final int DEFAULT_MAX_RETRY = 5;

    private final SearchSyncTaskMapper searchSyncTaskMapper;

    @Override
    public void enqueueTicketUpsert(Long ticketId) {
        BizAssert.notNull(ticketId, ErrorCode.PARAM_INVALID, "工单ID不能为空");

        SearchSyncTask task = new SearchSyncTask();
        task.setId(IdWorker.getId());
        task.setBizType(SearchSyncTaskConstants.BIZ_TYPE_TICKET);
        task.setBizId(ticketId);
        task.setOperation(SearchSyncTaskConstants.OPERATION_UPSERT);
        task.setDedupeKey(buildDedupeKey(
                SearchSyncTaskConstants.BIZ_TYPE_TICKET,
                ticketId,
                SearchSyncTaskConstants.OPERATION_UPSERT
        ));
        task.setStatus(SearchSyncTaskConstants.STATUS_PENDING);
        task.setRetryCount(0);
        task.setMaxRetry(DEFAULT_MAX_RETRY);
        task.setNextRetryAt(LocalDateTime.now());
        task.setLastError(null);

        searchSyncTaskMapper.upsertTask(task);
    }

    private String buildDedupeKey(String bizType, Long bizId, String operation) {
        return bizType + ":" + bizId + ":" + operation;
    }
}