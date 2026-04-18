package com.xcvk.platform.common.domain;

import com.xcvk.platform.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 统一返回格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result<T extends Serializable> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;

    public static <T extends Serializable> Result<T> success() {
        return new Result<>(
                ErrorCode.SUCCESS.getCode(),
                ErrorCode.SUCCESS.getMessage(),
                null
        );
    }

    public static <T extends Serializable> Result<T> success(T data) {
        return new Result<>(
                ErrorCode.SUCCESS.getCode(),
                ErrorCode.SUCCESS.getMessage(),
                data
        );
    }

    public static <T extends Serializable> Result<T> fail(ErrorCode errorCode) {
        return new Result<>(
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }

    public static <T extends Serializable> Result<T> fail(ErrorCode errorCode, String message) {
        return new Result<>(
                errorCode.getCode(),
                message,
                null
        );
    }

    public static <T extends Serializable> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}