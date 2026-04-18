package com.xcvk.platform.common.exception;

import lombok.Data;
import lombok.Getter;

/**
 * 业务异常
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18 10:19
 */
/**
 * 业务异常
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(IErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(IErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
