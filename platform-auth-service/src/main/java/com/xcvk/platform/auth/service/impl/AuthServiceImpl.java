package com.xcvk.platform.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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

import java.util.List;
import java.util.Objects;

/**
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18 10:58
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysDeptMapper sysDeptMapper;

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, request.username())
                        .last("limit 1")
        );

        if (user == null) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "用户名或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户已被禁用");
        }

        if (!PasswordUtils.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "用户名或密码错误");
        }

        List<String> roleCodes = getRoleCodes(user.getId());

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

    @Override
    public CurrentUserInfo getCurrentUser() {
        StpUtil.checkLogin();

        Long userId = Long.valueOf(String.valueOf(StpUtil.getLoginId()));

        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前登录状态已失效");
        }

        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户已被禁用");
        }

        List<String> roleCodes = getRoleCodes(user.getId());
        String deptName = getDeptName(user.getDeptId());

        return new CurrentUserInfo(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                deptName,
                roleCodes
        );
    }

    private List<String> getRoleCodes(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, userId)
        );

        if (userRoles == null || userRoles.isEmpty()) {
            return List.of();
        }

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .toList();

        List<SysRole> roles = sysRoleMapper.selectByIds(roleIds);
        if (roles == null || roles.isEmpty()) {
            return List.of();
        }

        return roles.stream()
                .filter(role -> role.getStatus() != null && role.getStatus() == 1)
                .map(SysRole::getRoleCode)
                .filter(Objects::nonNull)
                .toList();
    }

    private String getDeptName(Long deptId) {
        if (deptId == null) {
            return null;
        }

        SysDept dept = sysDeptMapper.selectById(deptId);
        if (dept == null || dept.getStatus() == null || dept.getStatus() != 1) {
            return null;
        }

        return dept.getDeptName();
    }
}