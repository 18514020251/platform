package com.xcvk.platform.log.starter.model;

import org.springframework.data.annotation.Id;

import java.time.Instant;

/**
 * 接口访问日志记录。
 *
 * <p>使用 record 表达不可变日志快照，避免为 MongoDB 写入对象维护大量 getter/setter。
 * 字段保持扁平化，便于按 traceId、服务名、URL、成功状态、耗时和请求时间进行查询。</p>
 *
 * @author Programmer
 * @version 1.1
 * @date 2026-05-11
 */
public record AccessLogRecord(
        @Id String id,
        String traceId,
        String serviceName,
        String operation,
        String httpMethod,
        String url,
        String classMethod,
        String clientIp,
        String userAgent,
        String userId,
        String args,
        String result,
        Boolean success,
        String errorType,
        String errorMessage,
        Long costMs,
        Instant requestTime,
        Instant createdAt
) {
}
