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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 *
 * <p>提供登录、登出、获取当前用户信息等认证相关接口。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18 10:56
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "认证管理", description = "用户登录、登出、获取当前用户信息")
@Validated
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     *
     * <p>根据用户名和密码完成登录认证，认证成功后返回 token 及当前用户基础信息。</p>
     * <p>此方法为对外暴露的核心业务接口，需要记录登录请求日志以便问题追踪。</p>
     *
     * @param request 登录请求参数
     * @return 登录结果，包含 token 和用户信息
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "使用用户名密码登录系统")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 用户登出
     *
     * <p>清除当前用户的登录态，使 token 失效。</p>
     *
     * @return 空结果
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "退出当前登录状态")
    public Result<Void> logout() {
        authService.logout();
        return Result.successVoid();
    }

    /**
     * 获取当前用户信息
     *
     * <p>获取已登录用户的详细信息，包括用户基本信息、角色列表、部门信息等。</p>
     * <p>前端可通过此接口获取当前用户权限和展示信息。</p>
     *
     * @return 当前用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取已登录用户的详细信息")
    public Result<CurrentUserInfo> me() {
        return Result.success(authService.getCurrentUser());
    }
}