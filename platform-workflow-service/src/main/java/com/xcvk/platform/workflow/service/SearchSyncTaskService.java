package com.xcvk.platform.workflow.service;

/**
 * 搜索索引同步任务服务。
 */
public interface SearchSyncTaskService {

    /**
     * 投递工单索引 upsert 任务。
     *
     * @param ticketId 工单ID
     */
    void enqueueTicketUpsert(Long ticketId);
}