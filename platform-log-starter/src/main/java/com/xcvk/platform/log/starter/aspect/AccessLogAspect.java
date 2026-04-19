package com.xcvk.platform.log.starter.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import com.xcvk.platform.log.starter.properties.AccessLogProperties;
import com.xcvk.platform.log.starter.support.AccessLogSanitizer;
import com.xcvk.platform.log.starter.util.IpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 访问日志切面
 *
 * <p>第一版聚焦基础访问日志能力：</p>
 * <ul>
 *     <li>记录 URL、HTTP Method、类方法、IP、操作描述</li>
 *     <li>记录请求耗时</li>
 *     <li>记录成功/失败结果</li>
 *     <li>对请求参数和返回值进行安全脱敏</li>
 * </ul>
 *
 * <p>这里不承载复杂业务审计逻辑，业务级日志仍建议在 Service 中按语义补充。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-19
 */
@Aspect
@Slf4j
@RequiredArgsConstructor
public class AccessLogAspect {

    private final ObjectMapper objectMapper;
    private final AccessLogSanitizer sanitizer;
    private final AccessLogProperties properties;

    @Around("@annotation(accessLog)")
    public Object around(ProceedingJoinPoint point, AccessLog accessLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        HttpServletRequest request = getCurrentRequest();
        String url = request != null ? request.getRequestURI() : "unknown";
        String httpMethod = request != null ? request.getMethod() : "unknown";
        String clientIp = IpUtils.getClientIp(request);
        String classMethod = point.getSignature().toShortString();
        String operation = accessLog.value();

        String argsText = "[skipped]";
        if (properties.isRecordArgs() && accessLog.recordArgs()) {
            argsText = toLogText(sanitizer.sanitizeArgs(point.getArgs()));
        }

        log.info("access start, operation={}, method={}, url={}, ip={}, classMethod={}, args={}",
                operation, httpMethod, url, clientIp, classMethod, argsText);

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

            if (throwable == null) {
                String resultText = "[skipped]";
                if (properties.isRecordResult() && accessLog.recordResult()) {
                    resultText = toLogText(sanitizer.sanitizeResult(result));
                }

                log.info("access finish, operation={}, method={}, url={}, cost={}ms, success=true, result={}",
                        operation, httpMethod, url, cost, resultText);
            } else {
                log.warn("access finish, operation={}, method={}, url={}, cost={}ms, success=false, errorType={}, errorMessage={}",
                        operation, httpMethod, url, cost,
                        throwable.getClass().getSimpleName(),
                        throwable.getMessage());
            }
        }
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

        return text.substring(0, maxLength) + "...(truncated)";
    }
}