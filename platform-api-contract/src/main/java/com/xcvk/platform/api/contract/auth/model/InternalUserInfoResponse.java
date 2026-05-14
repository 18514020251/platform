package com.xcvk.platform.api.contract.auth.model;

import java.util.List;

/**
 * 内部用户信息响应。
 *
 * <p>供其他服务通过内部接口查询用户基础信息和角色信息，
 * 不向前端直接暴露。</p>
 */
public record InternalUserInfoResponse(
        Long userId,
        String username,
        String realName,
        Long deptId,
        Integer status,
        List<String> roleCodes
) {
}