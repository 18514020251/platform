package com.xcvk.platform.common.util;

import com.xcvk.platform.common.exception.BusinessException;
import com.xcvk.platform.common.exception.ErrorCode;

/**
 * 数据库断言工具类
 *
 * <p>用于统一校验数据库写操作影响行数是否符合预期。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-21
 */
public final class DbAssert {

    private DbAssert() {
    }

    /**
     * 断言数据库操作影响 1 行
     *
     * @param rows 实际影响行数
     * @param message 错误信息
     */
    public static void affectedOne(int rows, String message) {
        if (rows != 1) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, message);
        }
    }
}