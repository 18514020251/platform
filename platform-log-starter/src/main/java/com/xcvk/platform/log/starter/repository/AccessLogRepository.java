package com.xcvk.platform.log.starter.repository;

import com.xcvk.platform.log.starter.model.AccessLogRecord;

/**
 * 访问日志存储接口。
 *
 * <p>starter 内部通过接口隔离具体存储实现，当前默认实现为 MongoDB。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-11
 */
public interface AccessLogRepository {

    /**
     * 保存接口访问日志。
     *
     * @param record 访问日志记录
     */
    void save(AccessLogRecord record);
}
