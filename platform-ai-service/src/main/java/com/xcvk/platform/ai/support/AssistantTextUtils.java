package com.xcvk.platform.ai.support;

import org.springframework.util.StringUtils;

/**
 * Assistant 文本工具类。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-09
 */
public final class AssistantTextUtils {

    private AssistantTextUtils() {
    }

    public static String safeText(String value, String fallback) {
        if (StringUtils.hasText(value)) {
            return value.trim();
        }
        return fallback;
    }

    public static String normalizeForKeywordMatch(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        return text.toLowerCase()
                .replaceAll("\\s+", "");
    }

    public static boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }

        String normalizedText = normalizeForKeywordMatch(text);

        for (String keyword : keywords) {
            String normalizedKeyword = normalizeForKeywordMatch(keyword);
            if (StringUtils.hasText(normalizedKeyword) && normalizedText.contains(normalizedKeyword)) {
                return true;
            }
        }

        return false;
    }

    public static String buildDefaultTitle(String question) {
        String normalized = question == null ? "待处理问题" : question.trim().replaceAll("\\s+", " ");

        if (normalized.length() <= 30) {
            return normalized;
        }

        return normalized.substring(0, 30);
    }

    public static String truncate(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return value;
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength);
    }
}