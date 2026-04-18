package com.xcvk.platform.auth.module.vo;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 当前登录用户信息
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18 10:55
 */
public record CurrentUserInfo(
        Long userId,
        String username,
        String realName,
        String deptName,
        List<String> roleCodes
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
