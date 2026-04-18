package com.xcvk.platform.auth.service;

import com.xcvk.platform.auth.model.dto.LoginRequest;
import com.xcvk.platform.auth.model.vo.CurrentUserInfo;
import com.xcvk.platform.auth.model.vo.LoginResponse;

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
