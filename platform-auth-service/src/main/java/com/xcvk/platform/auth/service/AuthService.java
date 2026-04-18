package com.xcvk.platform.auth.service;

import com.xcvk.platform.auth.module.dto.LoginRequest;
import com.xcvk.platform.auth.module.vo.CurrentUserInfo;
import com.xcvk.platform.auth.module.vo.LoginResponse;

/**
 * @author Programmer
 * @version 1.0
 * @date 2026-04-18 10:58
 */
public interface AuthService {

    LoginResponse login(LoginRequest request);

    void logout();

    CurrentUserInfo getCurrentUser();
}
