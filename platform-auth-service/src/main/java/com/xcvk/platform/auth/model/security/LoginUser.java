package com.xcvk.platform.auth.model.security;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 当前登录用户身份上下文
 *
 * <p>该对象用于承接登录后的核心身份信息，主要服务于后续鉴权和当前登录人识别，
 * 不直接等同于前端展示对象。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-19
 */
public record LoginUser(
        Long userId,
        String username,
        String realName,
        Long deptId,
        String deptName,
        List<String> roleCodes
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public LoginUser {
        roleCodes = roleCodes == null ? List.of() : List.copyOf(roleCodes);
    }
}