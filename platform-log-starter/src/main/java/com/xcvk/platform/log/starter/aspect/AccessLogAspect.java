package com.xcvk.platform.log.starter.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import com.xcvk.platform.log.starter.model.AccessLogRecord;
import com.xcvk.platform.log.starter.properties.AccessLogProperties;
import com.xcvk.platform.log.starter.support.AccessLogPublisher;
import com.xcvk.platform.log.starter.support.AccessLogSanitizer;
import com.xcvk.platform.log.starter.support.AccessLogUserResolver;
import com.xcvk.platform.log.starter.util.IpUtils;
import com.xcvk.platform.log.starter.util.TraceIdUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;

/**
 * 访问日志切面。
 *
 * <p>通过 @AccessLog 标记核心接口，统一记录 URL、HTTP Method、类方法、IP、用户、耗时、成功状态和异常信息。</p>
 *
 * <p>MDC 中写入 traceId、serviceName、userId，业务日志与异步 MongoDB 写入可以共享同一条链路标识。</p>
 *
 * @author Programmer
 * @version 1.2
 * @date 2026-05-11
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class AccessLogAspect {

    private static final String MDC_SERVICE_NAME = "serviceName";

    private static final String MDC_USER_ID = "userId";

    private final ObjectMapper objectMapper;
    private final AccessLogSanitizer sanitizer;
    private final AccessLogProperties properties;
    private final ObjectProvider<AccessLogPublisher> accessLogPublisherProvider;
    private final AccessLogUserResolver userResolver;

    @Value("${spring.application.name:unknown-service}")
    private String serviceName;

    @Around("@annotation(accessLog)")
    public Object around(ProceedingJoinPoint point, AccessLog accessLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        Instant requestTime = Instant.now();

        HttpServletRequest request = getCurrentRequest();
        String traceId = TraceIdUtils.getOrCreateTraceId(request);
        String userId = userResolver.resolveUserId();
        putMdc(traceId, serviceName, userId);

        String url = request != null ? request.getRequestURI() : "unknown";
        String httpMethod = request != null ? request.getMethod() : "unknown";
        String clientIp = IpUtils.getClientIp(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : null;
        String classMethod = point.getSignature().toShortString();
        String operation = accessLog.value();

        String argsText = "[已跳过]";
        if (properties.isRecordArgs() && accessLog.recordArgs()) {
            argsText = toLogText(sanitizer.sanitizeArgs(point.getArgs()));
        }

        log.info("访问开始, traceId={}, 服务={}, 操作={}, 请求方式={}, 请求路径={}, IP={}, 用户ID={}, 类方法={}, 参数={}",
                traceId, serviceName, operation, httpMethod, url, clientIp, userId, classMethod, argsText);

        Object result = null;
        Throwable throwable = null;

        try {
            result = point.proceed();
            return result;
        } catch (Throwable ex) {
            throwable = ex;
            throw ex;
        } finally {
            long cost = System.currentTimeMillis() - startTime;
            String resultText = "[已跳过]";
            if (throwable == null && properties.isRecordResult() && accessLog.recordResult()) {
                resultText = toLogText(sanitizer.sanitizeResult(result));
            }

            if (throwable == null) {
                log.info("访问完成, traceId={}, 服务={}, 操作={}, 请求方式={}, 请求路径={}, 耗时={}ms, 成功=true, 结果={}",
                        traceId, serviceName, operation, httpMethod, url, cost, resultText);
            } else {
                log.warn("访问完成, traceId={}, 服务={}, 操作={}, 请求方式={}, 请求路径={}, 耗时={}ms, 成功=false, 异常类型={}, 异常信息={}",
                        traceId, serviceName, operation, httpMethod, url, cost,
                        throwable.getClass().getSimpleName(),
                        throwable.getMessage());
            }

            publishAccessLog(traceId, operation, httpMethod, url, classMethod, clientIp, userAgent,
                    userId, argsText, resultText, throwable, cost, requestTime);
            clearMdc();
        }
    }

    private void publishAccessLog(String traceId,
                                  String operation,
                                  String httpMethod,
                                  String url,
                                  String classMethod,
                                  String clientIp,
                                  String userAgent,
                                  String userId,
                                  String argsText,
                                  String resultText,
                                  Throwable throwable,
                                  long cost,
                                  Instant requestTime) {
        AccessLogPublisher publisher = accessLogPublisherProvider.getIfAvailable();

        if (!properties.isMongoEnabled()) {
            log.debug("跳过保存访问日志到MongoDB, 原因=MongoDB未启用, traceId={}, 服务={}, 请求路径={}",
                    traceId, serviceName, url);
            return;
        }

        if (publisher == null) {
            log.warn("跳过保存访问日志到MongoDB, 原因=未找到AccessLogPublisher, traceId={}, 服务={}, 请求路径={}",
                    traceId, serviceName, url);
            return;
        }

        AccessLogRecord record = new AccessLogRecord(
                null,
                traceId,
                serviceName,
                operation,
                httpMethod,
                url,
                classMethod,
                clientIp,
                truncate(userAgent),
                userId,
                argsText,
                resultText,
                throwable == null,
                throwable == null ? null : throwable.getClass().getSimpleName(),
                throwable == null ? null : truncate(throwable.getMessage()),
                cost,
                requestTime,
                Instant.now()
        );

        log.debug("提交访问日志到MongoDB异步任务, traceId={}, 服务={}, 请求路径={}, 成功={}, 耗时={}ms",
                traceId, serviceName, url, throwable == null, cost);
        publisher.publish(record);
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        return attributes == null ? null : attributes.getRequest();
    }

    private String toLogText(Object value) {
        if (value == null) {
            return "null";
        }

        try {
            String json = objectMapper.writeValueAsString(value);
            return truncate(json);
        } catch (JsonProcessingException ex) {
            return truncate(String.valueOf(value));
        }
    }

    private String truncate(String text) {
        if (text == null) {
            return null;
        }

        int maxLength = properties.getMaxContentLength();
        if (text.length() <= maxLength) {
            return text;
        }

        return text.substring(0, maxLength) + "...(已截断)";
    }

    private void putMdc(String traceId, String serviceName, String userId) {
        MDC.put(TraceIdUtils.TRACE_ID_MDC_KEY, traceId);
        MDC.put(MDC_SERVICE_NAME, serviceName);
        if (StringUtils.hasText(userId)) {
            MDC.put(MDC_USER_ID, userId);
        }
    }

    private void clearMdc() {
        MDC.remove(TraceIdUtils.TRACE_ID_MDC_KEY);
        MDC.remove(MDC_SERVICE_NAME);
        MDC.remove(MDC_USER_ID);
    }
}