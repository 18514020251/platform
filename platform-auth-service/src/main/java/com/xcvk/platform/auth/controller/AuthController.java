package com.xcvk.platform.auth.controller;

import com.xcvk.platform.auth.model.dto.LoginRequest;
import com.xcvk.platform.auth.model.vo.CurrentUserInfo;
import com.xcvk.platform.auth.model.vo.LoginResponse;
import com.xcvk.platform.auth.service.AuthService;
import com.xcvk.platform.common.domain.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18 10:56
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、登出、获取当前用户信息")
@Validated
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名密码登录系统")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("收到登录请求: username={}", request.username());  // 加这行
        return Result.success(authService.login(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "退出当前登录状态")
    public Result<Void> logout() {
        authService.logout();
        return Result.successVoid();
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取已登录用户的详细信息")
    public Result<CurrentUserInfo> me() {
        return Result.success(authService.getCurrentUser());
    }
}