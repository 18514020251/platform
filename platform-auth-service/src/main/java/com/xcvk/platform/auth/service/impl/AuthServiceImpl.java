package com.xcvk.platform.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xcvk.platform.auth.enums.CommonStatusEnum;
import com.xcvk.platform.auth.model.dto.LoginRequest;
import com.xcvk.platform.auth.model.entity.SysDept;
import com.xcvk.platform.auth.model.entity.SysRole;
import com.xcvk.platform.auth.model.entity.SysUser;
import com.xcvk.platform.auth.model.entity.SysUserRole;
import com.xcvk.platform.auth.model.vo.CurrentUserInfo;
import com.xcvk.platform.auth.model.vo.LoginResponse;
import com.xcvk.platform.auth.repository.mapper.SysDeptMapper;
import com.xcvk.platform.auth.repository.mapper.SysRoleMapper;
import com.xcvk.platform.auth.repository.mapper.SysUserMapper;
import com.xcvk.platform.auth.repository.mapper.SysUserRoleMapper;
import com.xcvk.platform.auth.service.AuthService;
import com.xcvk.platform.common.exception.BusinessException;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.PasswordUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 认证服务实现类
 *
 * <p>负责登录认证、登录态退出和当前用户信息获取。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18 10:58
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String LOGIN_FAILED_MESSAGE = "用户名或密码错误";
    private static final String USER_DISABLED_MESSAGE = "用户已被禁用";
    private static final String LOGIN_EXPIRED_MESSAGE = "当前登录状态已失效";

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysDeptMapper sysDeptMapper;

    /**
     * 登录主流程只保留认证链路本身：
     * 先确认账号是否可登录，再校验密码，最后补充角色信息并建立登录态。
     *
     * <p>这里对“用户不存在”和“密码错误”统一返回相同提示，
     * 是为了避免把账号是否存在暴露给外部调用方。</p>
     *
     * @param request 登录请求
     * @return 登录结果
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = findUserByUsername(request.username());

        validateLoginUser(user);
        validatePassword(request.password(), user.getPassword());

        List<String> roleCodes = getEnabledRoleCodes(user.getId());

        StpUtil.login(user.getId());

        return new LoginResponse(
                StpUtil.getTokenValue(),
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                roleCodes
        );
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 获取当前用户信息时，需要再次校验用户是否仍然有效，
     * 防止“用户已被删除或禁用，但历史登录态尚未失效”的情况继续访问系统。
     *
     * <p>部门名称属于展示增强字段，不是登录主链字段；
     * 即使部门不存在或已禁用，也不应影响当前接口主流程返回。</p>
     *
     * @return 当前登录用户信息
     */
    @Override
    public CurrentUserInfo getCurrentUser() {
        StpUtil.checkLogin();

        Long userId = getCurrentLoginUserId();
        SysUser user = findUserById(userId);

        validateCurrentUser(user);

        List<String> roleCodes = getEnabledRoleCodes(user.getId());
        String deptName = getDeptName(user.getDeptId());

        return new CurrentUserInfo(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                deptName,
                roleCodes
        );
    }

    private SysUser findUserByUsername(String username) {
        return sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, username)
        );
    }

    private SysUser findUserById(Long userId) {
        return sysUserMapper.selectById(userId);
    }

    private Long getCurrentLoginUserId() {
        return Long.valueOf(String.valueOf(StpUtil.getLoginId()));
    }

    /**
     * 登录场景下只关心“这个账号是否允许继续认证”，
     * 其中“账号不存在”和“密码错误”都不对外暴露具体差异。
     *
     * @param user 用户信息
     */
    private void validateLoginUser(SysUser user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, LOGIN_FAILED_MESSAGE);
        }

        if (!CommonStatusEnum.isEnabled(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, USER_DISABLED_MESSAGE);
        }
    }

    /**
     * 已登录场景下需要区分两类问题：
     * 一类是登录态对应的用户已经不存在，说明当前登录态应视为失效；
     * 另一类是用户仍存在但状态被禁用，此时应明确返回无权限访问。
     *
     * @param user 用户信息
     */
    private void validateCurrentUser(SysUser user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, LOGIN_EXPIRED_MESSAGE);
        }

        if (!CommonStatusEnum.isEnabled(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, USER_DISABLED_MESSAGE);
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!PasswordUtils.matches(rawPassword, encodedPassword)) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, LOGIN_FAILED_MESSAGE);
        }
    }

    /**
     * 角色查询分两步走：
     * 先查用户-角色关系，再批量查角色表，只返回当前仍处于启用状态的角色编码。
     *
     * <p>这里额外做了去重和空值过滤，避免脏数据直接进入登录态返回结果。</p>
     *
     * @param userId 用户ID
     * @return 启用状态的角色编码列表
     */
    private List<String> getEnabledRoleCodes(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, userId)
        );

        if (userRoles == null || userRoles.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .distinct()
                .toList();

        List<SysRole> roles = sysRoleMapper.selectByIds(roleIds);
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        return roles.stream()
                .filter(role -> CommonStatusEnum.isEnabled(role.getStatus()))
                .map(SysRole::getRoleCode)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String getDeptName(Long deptId) {
        if (deptId == null) {
            return null;
        }

        SysDept dept = sysDeptMapper.selectById(deptId);
        if (dept == null || !CommonStatusEnum.isEnabled(dept.getStatus())) {
            return null;
        }

        return dept.getDeptName();
    }
}