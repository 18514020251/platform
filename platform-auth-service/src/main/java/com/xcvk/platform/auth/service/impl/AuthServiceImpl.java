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

import java.util.List;
import java.util.Objects;

/**
 * 认证服务实现类
 *
 * <p>实现用户登录、登出、获取当前用户信息等核心认证逻辑。</p>
 *
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

    /**
     * 用户登录
     *
     * <p>登录核心流程：</p>
     * <ol>
     *   <li>按用户名查询用户</li>
     *   <li>校验用户是否存在且状态为启用</li>
     *   <li>校验密码是否正确</li>
     *   <li>查询用户角色编码列表（用于后续权限控制）</li>
     *   <li>调用 Sa-Token 进行登录，生成 token</li>
     *   <li>构建并返回登录响应</li>
     * </ol>
     *
     * @param request 登录请求参数
     * @return 登录响应结果
     */
    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = getUserByUsername(request.username());

        validateLoginUser(user);
        validatePassword(request.password(), user.getPassword());

        List<String> roleCodes = getRoleCodes(user.getId());

        StpUtil.login(user.getId());

        return buildLoginResponse(user, roleCodes);
    }

    /**
     * 用户登出
     *
     * <p>调用 Sa-Token 登出方法，清除当前用户的登录态。</p>
     */
    @Override
    public void logout() {
        StpUtil.logout();
    }

    /**
     * 获取当前登录用户信息
     *
     * <p>获取流程：</p>
     * <ol>
     *   <li>校验当前是否有登录态</li>
     *   <li>从登录态中获取用户 ID</li>
     *   <li>查询用户详细信息</li>
     *   <li>校验用户状态（防止登录后被禁用的情况）</li>
     *   <li>补充角色编码列表和部门名称</li>
     *   <li>构建并返回当前用户信息</li>
     * </ol>
     *
     * @return 当前登录用户信息
     */
    @Override
    public CurrentUserInfo getCurrentUser() {
        StpUtil.checkLogin();

        Long userId = Long.valueOf(String.valueOf(StpUtil.getLoginId()));
        SysUser user = getUserById(userId);

        validateCurrentUser(user);

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

    // ==================== 私有辅助方法 ====================

    private SysUser getUserByUsername(String username) {
        return sysUserMapper.selectOne(
                Wrappers.<SysUser>lambdaQuery()
                        .eq(SysUser::getUsername, username)
        );
    }

    private SysUser getUserById(Long userId) {
        return sysUserMapper.selectById(userId);
    }

    /**
     * 校验登录时的用户状态
     *
     * <p>校验项：用户是否存在、用户是否被禁用</p>
     * <p>注意：此处不区分用户不存在和密码错误的具体原因，统一返回模糊提示，防止用户名枚举攻击。</p>
     *
     * @param user 用户实体，可能为 null
     */
    private void validateLoginUser(SysUser user) {
        if (user == null) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "用户名或密码错误");
        }

        if (!CommonStatusEnum.isEnabled(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户已被禁用");
        }
    }

    /**
     * 校验当前用户状态
     *
     * <p>与登录校验的区别：登录失效返回 UNAUTHORIZED，禁用返回 FORBIDDEN。</p>
     *
     * @param user 用户实体，可能为 null
     */
    private void validateCurrentUser(SysUser user) {
        if (user == null) {
            // 用户被删除后，登录态还存在的情况
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "当前登录状态已失效");
        }

        if (!CommonStatusEnum.isEnabled(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "用户已被禁用");
        }
    }

    /**
     * 校验密码
     *
     * <p>使用 PasswordUtils 进行密码匹配（支持 BCrypt 等加密方式）。</p>
     * <p>密码错误时与用户不存在返回相同的错误信息，防止用户名枚举攻击。</p>
     *
     * @param rawPassword    前端传入的明文密码
     * @param encodedPassword 数据库中存储的加密密码
     */
    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!PasswordUtils.matches(rawPassword, encodedPassword)) {
            throw new BusinessException(ErrorCode.BIZ_ERROR, "用户名或密码错误");
        }
    }

    /**
     * 构建登录响应
     *
     * @param user      用户实体
     * @param roleCodes 用户角色编码列表
     * @return 登录响应对象
     */
    private LoginResponse buildLoginResponse(SysUser user, List<String> roleCodes) {
        return new LoginResponse(
                StpUtil.getTokenValue(),
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                roleCodes
        );
    }

    /**
     * 查询用户角色编码列表
     *
     * <p>查询逻辑：</p>
     * <ol>
     *   <li>通过用户角色关联表查询用户关联的角色 ID</li>
     *   <li>通过角色 ID 批量查询角色信息</li>
     *   <li>过滤掉禁用状态的角色（禁用角色不应赋予权限）</li>
     *   <li>提取角色编码，并过滤掉空值</li>
     * </ol>
     *
     * @param userId 用户ID
     * @return 角色编码列表，无角色时返回空列表
     */
    private List<String> getRoleCodes(Long userId) {
        // 查询用户关联的角色关系
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                Wrappers.<SysUserRole>lambdaQuery()
                        .eq(SysUserRole::getUserId, userId)
        );

        if (userRoles.isEmpty()) {
            return List.of();
        }

        // 提取角色 ID 列表
        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .toList();

        List<SysRole> roles = sysRoleMapper.selectByIds(roleIds);
        if (roles.isEmpty()) {
            return List.of();
        }

        // 过滤启用状态的角色，提取角色编码
        return roles.stream()
                .filter(role -> CommonStatusEnum.isEnabled(role.getStatus()))
                .map(SysRole::getRoleCode)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 获取部门名称
     *
     * <p>根据部门 ID 查询部门名称，如果部门不存在或被禁用，返回 null。</p>
     * <p>部门信息非核心字段，缺失时返回 null 不影响主流程。</p>
     *
     * @param deptId 部门 ID，可能为 null
     * @return 部门名称，不存在或禁用时返回 null
     */
    private String getDeptName(Long deptId) {
        if (deptId == null) {
            return null;
        }

        SysDept dept = sysDeptMapper.selectById(deptId);
        // 部门不存在或已禁用时，不返回部门名称
        if (dept == null || !CommonStatusEnum.isEnabled(dept.getStatus())) {
            return null;
        }

        return dept.getDeptName();
    }
}