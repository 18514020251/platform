package com.xcvk.platform.auth.starter.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sa-Token 异常处理器
 *
 * <p>用于统一处理认证、角色、权限相关异常，
 * 避免这类“可预期的访问控制失败”继续落入全局兜底异常处理，
 * 被误判为系统异常。</p>
 *
 * <p>当前阶段统一处理三类典型场景：</p>
 * <ul>
 *     <li>未登录或登录失效</li>
 *     <li>角色校验失败</li>
 *     <li>权限校验失败</li>
 * </ul>
 *
 * <p>这样做的目的是让前端能够明确区分：
 * 当前是登录态问题，还是访问权限问题，
 * 而不是统一收到“系统异常”的模糊提示。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-21
 */
@Slf4j
@RestControllerAdvice
public class SaTokenExceptionHandler {

    /**
     * 处理未登录或登录失效异常。
     *
     * <p>这类异常属于认证失败，不是系统错误。
     * 因此这里返回明确的未登录提示，便于前端统一做登录跳转或 token 失效处理。</p>
     *
     * @param e 未登录异常
     * @return 统一响应
     */
    @ExceptionHandler(NotLoginException.class)
    public Result<Void> handleNotLoginException(NotLoginException e) {
        log.warn("认证异常: type={}, message={}", e.getType(), e.getMessage());
        return Result.fail(ErrorCode.UNAUTHORIZED, "未登录或登录已失效");
    }

    /**
     * 处理角色校验失败异常。
     *
     * <p>当接口通过 @SaCheckRole 做角色准入控制时，
     * 如果当前用户不具备要求角色，应明确返回无权限提示，
     * 而不是继续按系统异常处理。</p>
     *
     * @param e 角色校验异常
     * @return 统一响应
     */
    @ExceptionHandler(NotRoleException.class)
    public Result<Void> handleNotRoleException(NotRoleException e) {
        log.warn("角色校验异常: role={}, message={}", e.getRole(), e.getMessage());
        return Result.fail(ErrorCode.FORBIDDEN, "无权限访问该接口");
    }

    /**
     * 处理权限校验失败异常。
     *
     * <p>该异常通常来自更细粒度的权限点控制，
     * 语义上同样属于禁止访问，因此统一返回无权限提示。</p>
     *
     * @param e 权限校验异常
     * @return 统一响应
     */
    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> handleNotPermissionException(NotPermissionException e) {
        log.warn("权限校验异常: permission={}, message={}", e.getPermission(), e.getMessage());
        return Result.fail(ErrorCode.FORBIDDEN, "无权限访问该接口");
    }
}