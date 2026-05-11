package com.xcvk.platform.log.model.vo;

import java.time.Instant;

/**
 * 接口访问日志列表项。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-11
 */
public record AccessLogItemVO(
        String id,
        String traceId,
        String serviceName,
        String operation,
        String httpMethod,
        String url,
        String classMethod,
        String clientIp,
        String userAgent,
        String userId,
        Boolean success,
        String errorType,
        String errorMessage,
        Long costMs,
        Instant requestTime,
        Instant createdAt
) {
}
