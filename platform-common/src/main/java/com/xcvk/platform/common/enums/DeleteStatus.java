package com.xcvk.platform.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 逻辑删除枚举类
 *
 * @author Programmer
 * @date 2026-01-16 17:53
 */
@Getter
public enum DeleteStatus {
    NORMAL(0, "正常"),
    DELETED(1, "已删除");

    @EnumValue
    private final Integer code;

    private final String desc;

    DeleteStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

}
