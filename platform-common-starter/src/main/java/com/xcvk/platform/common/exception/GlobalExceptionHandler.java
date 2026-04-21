package com.xcvk.platform.common.exception;

import com.xcvk.platform.common.domain.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


/**
 * 全局异常处理器
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18 10:17
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.warn("业务异常，code={}, message={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /**
     * 处理 @RequestBody 参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : ErrorCode.PARAM_INVALID.getMessage();

        log.warn("参数校验异常: {}", message);
        return Result.fail(ErrorCode.PARAM_INVALID, message);
    }

    /**
     * 处理表单绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : ErrorCode.PARAM_INVALID.getMessage();

        log.warn("参数绑定异常: {}", message);
        return Result.fail(ErrorCode.PARAM_INVALID, message);
    }

    /**
     * 处理单参数校验异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        log.warn("参数校验异常: {}", e.getMessage());
        return Result.fail(ErrorCode.PARAM_INVALID, e.getMessage());
    }

    /**
     * 处理请求体缺失或格式错误异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("请求体缺失或格式错误: {}", e.getMessage());

        String message = "请求参数格式错误";
        if (e.getMessage() != null) {
            if (e.getMessage().contains("Required request body is missing")) {
                message = "请求体不能为空";
            } else if (e.getMessage().contains("JSON parse error")) {
                message = "JSON格式错误";
            }
        }

        return Result.fail(ErrorCode.PARAM_INVALID, message);
    }

    /**
     * 处理其他未捕获异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ErrorCode.SYSTEM_ERROR);
    }
}