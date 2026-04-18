package com.xcvk.platform.auth.module.dto;

import jakarta.validation.constraints.NotBlank;

import java.io.Serial;
import java.io.Serializable;

/**
 * 登录请求
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18 10:42
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
