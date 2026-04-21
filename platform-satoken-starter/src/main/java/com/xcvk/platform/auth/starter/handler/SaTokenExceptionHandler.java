package com.xcvk.platform.auth.starter.handler;

import cn.dev33.satoken.exception.NotLoginException;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 异常处理器
 *
 * <p>统一处理登录态相关异常，避免在公共模块中直接依赖 Sa-Token。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-21
 */
@Slf4j
@RestControllerAdvice
public class SaTokenExceptionHandler {

    /**
     * 处理未登录异常
     *
     * @param e 未登录异常
     * @return 统一返回结果
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.warn("未登录异常: {}", e.getMessage());
        return Result.fail(ErrorCode.UNAUTHORIZED);
    }
}