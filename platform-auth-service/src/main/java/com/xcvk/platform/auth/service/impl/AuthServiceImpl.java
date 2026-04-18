package com.xcvk.platform.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.xcvk.platform.auth.module.dto.LoginRequest;
import com.xcvk.platform.auth.module.vo.CurrentUserInfo;
import com.xcvk.platform.auth.module.vo.LoginResponse;
import com.xcvk.platform.auth.service.AuthService;
import com.xcvk.platform.common.exception.BusinessException;
import com.xcvk.platform.common.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18 10:58
 */
@Service
public class AuthServiceImpl implements AuthService {

    @Override
    public LoginResponse login(LoginRequest request) {
        MockUser mockUser = validateUser(request);

        StpUtil.login(mockUser.userId());

        return new LoginResponse(
                StpUtil.getTokenValue(),
                mockUser.userId(),
                mockUser.username(),
                mockUser.realName(),
                mockUser.roleCodes()
        );
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public CurrentUserInfo getCurrentUser() {
        StpUtil.checkLogin();

        Object loginId = StpUtil.getLoginId();
        Long userId = Long.valueOf(String.valueOf(loginId));

        MockUser mockUser = getMockUserById(userId);
        if (mockUser == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录用户不存在或登录已失效");
        }

        return new CurrentUserInfo(
                mockUser.userId(),
                mockUser.username(),
                mockUser.realName(),
                mockUser.deptName(),
                mockUser.roleCodes()
        );
    }

    private MockUser validateUser(LoginRequest request) {
        if ("admin".equals(request.username()) && "123456".equals(request.password())) {
            return new MockUser(
                    1L,
                    "admin",
                    "系统管理员",
                    "平台部",
                    List.of("ADMIN")
            );
        }

        if ("employee".equals(request.username()) && "123456".equals(request.password())) {
            return new MockUser(
                    2L,
                    "employee",
                    "普通员工",
                    "研发部",
                    List.of("EMPLOYEE")
            );
        }

        throw new BusinessException(ErrorCode.BIZ_ERROR, "用户名或密码错误");
    }

    private MockUser getMockUserById(Long userId) {
        if (1L == userId) {
            return new MockUser(
                    1L,
                    "admin",
                    "系统管理员",
                    "平台部",
                    List.of("ADMIN")
            );
        }

        if (2L == userId) {
            return new MockUser(
                    2L,
                    "employee",
                    "普通员工",
                    "研发部",
                    List.of("EMPLOYEE")
            );
        }

        return null;
    }

    /**
     * mock 用户
     */
    private record MockUser(
            Long userId,
            String username,
            String realName,
            String deptName,
            List<String> roleCodes
    ) {
    }
}
