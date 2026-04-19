package com.xcvk.platform.log.starter.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * IP 工具类
 *
 * <p>优先从代理头中提取真实客户端 IP，
 * 兼容 Gateway / Nginx / 反向代理后的常见场景。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-19
 */
public final class IpUtils {

    private static final String UNKNOWN = "unknown";

    private IpUtils() {
    }

    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        String ip = getHeader(request, "X-Forwarded-For");
        if (StringUtils.hasText(ip)) {
            int index = ip.indexOf(',');
            return index > 0 ? ip.substring(0, index).trim() : ip.trim();
        }

        ip = getHeader(request, "X-Real-IP");
        if (StringUtils.hasText(ip)) {
            return ip.trim();
        }

        return request.getRemoteAddr();
    }

    private static String getHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        if (!StringUtils.hasText(value)) {
            return null;
        }

        if (UNKNOWN.equalsIgnoreCase(value)) {
            return null;
        }

        return value;
    }
}