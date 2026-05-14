package com.xcvk.platform.auth.controller;

import com.xcvk.platform.api.contract.auth.model.InternalUserInfoResponse;
import com.xcvk.platform.auth.service.AuthService;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证服务内部用户接口。
 *
 * <p>该接口仅供系统内部服务调用，通过 /internal/** 路径触发内部 token 校验。</p>
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class AuthInternalUserController {

    private final AuthService authService;

    /**
     * 根据用户ID查询内部用户信息。
     *
     * @param userId 用户ID
     * @return 用户基础信息和角色编码
     */
    @GetMapping("/{userId}")
    @AccessLog(value = "内部查询用户信息", recordArgs = false, recordResult = false)
    @Operation(summary = "内部查询用户信息", description = "根据用户ID查询内部用户信息")
    public Result<InternalUserInfoResponse> getUserById(@PathVariable("userId") Long userId) {
        return Result.success(authService.getInternalUserInfo(userId));
    }
}