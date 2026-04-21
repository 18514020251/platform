package com.xcvk.platform.common.util;

import com.xcvk.platform.common.exception.BusinessException;
import com.xcvk.platform.common.exception.ErrorCode;
import org.springframework.util.StringUtils;

/**
 * 业务断言工具类
 *
 * <p>用于统一处理常见业务校验失败场景，减少重复的异常抛出代码。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-21
 */
public final class BizAssert {

    private BizAssert() {
    }

    /**
     * 断言对象不为空
     *
     * @param value 待校验对象
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public static void notNull(Object value, ErrorCode errorCode, String message) {
        if (value == null) {
            throw new BusinessException(errorCode, message);
        }
    }

    /**
     * 断言字符串有文本内容
     *
     * @param value 待校验字符串
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public static void hasText(String value, ErrorCode errorCode, String message) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(errorCode, message);
        }
    }

    /**
     * 断言条件为 true
     *
     * @param condition 条件
     * @param errorCode 错误码
     * @param message 错误信息
     */
    public static void isTrue(boolean condition, ErrorCode errorCode, String message) {
        if (!condition) {
            throw new BusinessException(errorCode, message);
        }
    }
}