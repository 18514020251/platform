package com.xcvk.platform.auth.starter.security;

import cn.dev33.satoken.stp.StpInterface;
import com.xcvk.platform.auth.starter.util.SaTokenSessionUtils;

import java.util.List;

/**
 * 基于 Sa-Token Session 的鉴权实现
 *
 * <p>当前阶段角色信息统一从 Sa-Token Session 中读取，
 * 这样各业务服务无需依赖 auth-service 的表结构和 mapper，
 * 即可直接使用 @SaCheckRole 进行角色鉴权。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public class SessionStpInterface implements StpInterface {

    private final SaTokenSessionUtils saTokenSessionUtils;

    public SessionStpInterface(SaTokenSessionUtils saTokenSessionUtils) {
        this.saTokenSessionUtils = saTokenSessionUtils;
    }

    /**
     * 当前阶段暂未接入细粒度 permission 模型，
     * 因此先返回空列表，后续做 @SaCheckPermission 时再补齐。
     *
     * @param loginId 账号id
     * @param loginType 账号体系标识
     * @return 权限码列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return List.of();
    }

    /**
     * 返回指定账号的角色编码列表
     *
     * <p>角色信息由登录成功时写入 Session，
     * 后续所有服务均可通过统一的 Session 读取逻辑完成角色注解鉴权。</p>
     *
     * @param loginId 账号id
     * @param loginType 账号体系标识
     * @return 角色编码列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return saTokenSessionUtils.getRoleCodesByLoginId(loginId);
    }
}