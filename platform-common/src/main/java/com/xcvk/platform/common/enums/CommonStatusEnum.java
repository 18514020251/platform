package com.xcvk.platform.common.enums;

import lombok.Getter;

import java.util.Arrays;

/**
 * 通用启用状态枚举
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18
 */
@Getter
public enum CommonStatusEnum {

    DISABLED(0, "禁用"),
    ENABLED(1, "启用");

    private final Integer code;
    private final String desc;

    CommonStatusEnum(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 判断是否为启用状态
     *
     * @param code 状态码
     * @return 是否启用
     */
    public static boolean isEnabled(Integer code) {
        return ENABLED.code.equals(code);
    }

    /**
     * 根据状态码获取枚举
     *
     * @param code 状态码
     * @return 枚举
     */
    public static CommonStatusEnum fromCode(Integer code) {
        return Arrays.stream(values())
                .filter(item -> item.code.equals(code))
                .findFirst()
                .orElse(null);
    }
}