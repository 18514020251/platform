package com.xcvk.platform.log.model.query;

import java.time.LocalDateTime;

/**
 * 接口访问日志分页查询条件。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-11
 */
public record AccessLogPageQuery(
        long pageNum,
        long pageSize,
        String traceId,
        String serviceName,
        String url,
        String userId,
        Boolean success,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
    public long safePageNum() {
        return pageNum <= 0 ? 1 : pageNum;
    }

    public long safePageSize() {
        if (pageSize <= 0) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }
}
