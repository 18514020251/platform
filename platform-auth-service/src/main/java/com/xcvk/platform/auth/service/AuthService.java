package com.xcvk.platform.auth.service;

import com.xcvk.platform.auth.model.dto.LoginRequest;
import com.xcvk.platform.auth.model.vo.CurrentUserInfo;
import com.xcvk.platform.auth.model.vo.LoginResponse;

/**
 * 认证服务接口
 *
 * <p>定义用户认证相关的核心业务能力，包括登录、登出、获取当前用户信息。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18 10:58
 */
public interface AuthService {

    /**
     * 用户登录
     *
     * <p>执行登录认证流程：校验用户名密码、验证用户状态、生成 token、返回用户信息。</p>
     * <p>认证失败会抛出业务异常，由全局异常处理器统一处理。</p>
     *
     * @param request 登录请求，包含用户名和密码
     * @return 登录响应，包含 token 和用户基本信息
     * @throws com.xcvk.platform.common.exception.BusinessException 用户名或密码错误、用户被禁用时抛出
     */
    LoginResponse login(LoginRequest request);

    /**
     * 用户登出
     *
     * <p>清除当前用户的登录态，使 token 立即失效。</p>
     */
    void logout();

    /**
     * 获取当前登录用户信息
     *
     * <p>从 Sa-Token 登录态中获取当前用户 ID，然后查询完整的用户信息。</p>
     * <p>未登录状态下调用此方法会抛出未授权异常。</p>
     *
     * @return 当前登录用户的完整信息
     * @throws com.xcvk.platform.common.exception.BusinessException 未登录或用户状态异常时抛出
     */
    CurrentUserInfo getCurrentUser();
}