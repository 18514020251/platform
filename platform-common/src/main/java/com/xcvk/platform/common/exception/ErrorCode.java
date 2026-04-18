package com.xcvk.platform.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 通用错误码
 */
public enum ErrorCode implements IErrorCode {

    SUCCESS(200, "success", HttpStatus.OK),
    PARAM_INVALID(400, "参数不合法", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED(401, "未登录或登录失效", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "无权限访问", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "未找到该资源", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED(405, "不支持当前请求方法", HttpStatus.METHOD_NOT_ALLOWED),
    REQUEST_TOO_FREQUENT(429, "请求过于频繁", HttpStatus.TOO_MANY_REQUESTS),
    BIZ_ERROR(400, "业务异常", HttpStatus.BAD_REQUEST),
    SYSTEM_ERROR(500, "系统异常", HttpStatus.INTERNAL_SERVER_ERROR),
    SERVICE_UNAVAILABLE(503, "服务不可用，请稍后重试", HttpStatus.SERVICE_UNAVAILABLE);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
