package com.xcvk.platform.log.starter.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * TraceId 工具类。
 *
 * <p>优先复用网关或上游传入的链路 ID，没有时生成本地 ID，方便接口日志串联排查。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-11
 */
public final class TraceIdUtils {

    public static final String TRACE_ID_MDC_KEY = "traceId";

    private TraceIdUtils() {
    }

    public static String getOrCreateTraceId(HttpServletRequest request) {
        String traceId = getHeader(request, "X-Trace-Id");
        if (!StringUtils.hasText(traceId)) {
            traceId = getHeader(request, "X-Request-Id");
        }
        if (!StringUtils.hasText(traceId)) {
            traceId = MDC.get(TRACE_ID_MDC_KEY);
        }
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        return traceId;
    }

    private static String getHeader(HttpServletRequest request, String headerName) {
        if (request == null) {
            return null;
        }
        String value = request.getHeader(headerName);
        return StringUtils.hasText(value) ? value : null;
    }
}
