package com.xcvk.platform.auth.starter.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 当前登录身份信息
 *
 * <p>用于在各业务服务中统一获取当前登录人的轻量身份上下文。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public record CurrentLoginIdentity(
        Long userId,
        String username,
        String realName,
        List<String> roleCodes
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public CurrentLoginIdentity {
        roleCodes = roleCodes == null ? List.of() : List.copyOf(roleCodes);
    }
}