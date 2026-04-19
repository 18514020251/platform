package com.xcvk.platform.log.starter.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcvk.platform.log.starter.properties.AccessLogProperties;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Array;
import java.util.*;

/**
 * 日志脱敏与安全序列化处理器
 *
 * <p>访问日志不应直接序列化所有入参和返回值。
 * 这里统一负责：</p>
 * <ul>
 *     <li>过滤 request / response / file 等不适合直接输出的对象</li>
 *     <li>对 password、token 等敏感字段做脱敏</li>
 *     <li>把普通对象转换成适合日志打印的结构</li>
 * </ul>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-19
 */
@RequiredArgsConstructor
public class AccessLogSanitizer {

    private static final String MASK = "******";

    private final ObjectMapper objectMapper;
    private final AccessLogProperties properties;

    public List<Object> sanitizeArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return List.of();
        }

        List<Object> result = new ArrayList<>(args.length);
        for (Object arg : args) {
            if (shouldSkip(arg)) {
                continue;
            }
            result.add(sanitizeValue(arg));
        }
        return result;
    }

    public Object sanitizeResult(Object result) {
        if (result == null) {
            return null;
        }
        return sanitizeValue(result);
    }

    private Object sanitizeValue(Object source) {
        if (source == null) {
            return null;
        }

        if (source instanceof String
                || source instanceof Number
                || source instanceof Boolean
                || source instanceof Enum<?>) {
            return source;
        }

        if (source instanceof MultipartFile file) {
            return Map.of(
                    "fileName", file.getOriginalFilename(),
                    "size", file.getSize(),
                    "contentType", file.getContentType()
            );
        }

        if (source instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::sanitizeValue)
                    .toList();
        }

        if (source.getClass().isArray()) {
            int length = Array.getLength(source);
            List<Object> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(sanitizeValue(Array.get(source, i)));
            }
            return list;
        }

        if (source instanceof Map<?, ?> map) {
            return sanitizeMap(map);
        }

        try {
            Object normalized = objectMapper.convertValue(source, Object.class);
            return sanitizeNormalized(normalized);
        } catch (IllegalArgumentException ex) {
            return String.valueOf(source);
        }
    }

    private Object sanitizeNormalized(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Enum<?>) {
            return value;
        }

        if (value instanceof Map<?, ?> map) {
            return sanitizeMap(map);
        }

        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::sanitizeNormalized)
                    .toList();
        }

        return String.valueOf(value);
    }

    private Map<String, Object> sanitizeMap(Map<?, ?> rawMap) {
        Map<String, Object> target = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (isSensitiveField(key)) {
                target.put(key, MASK);
                continue;
            }
            target.put(key, sanitizeNormalized(entry.getValue()));
        }
        return target;
    }

    private boolean isSensitiveField(String fieldName) {
        if (fieldName == null) {
            return false;
        }

        for (String sensitiveField : properties.getSensitiveFields()) {
            if (fieldName.equalsIgnoreCase(sensitiveField)) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldSkip(Object arg) {
        return arg == null
                || arg instanceof ServletRequest
                || arg instanceof ServletResponse
                || arg instanceof BindingResult;
    }
}