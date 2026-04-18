package com.xcvk.platform.auth.module.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 登录响应
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18 10:48
 */
public record LoginResponse(
        String token,
        Long userId,
        String username,
        String realName,
        List<String> roleCodes
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
