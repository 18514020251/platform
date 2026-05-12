package com.xcvk.platform.common.starter.internal;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

public class InternalApiInterceptor implements HandlerInterceptor {

    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final InternalApiProperties properties;

    public InternalApiInterceptor(InternalApiProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws Exception {
        String expectedToken = properties.getToken();
        String actualToken = request.getHeader(INTERNAL_TOKEN_HEADER);

        if (!StringUtils.hasText(expectedToken) || !expectedToken.equals(actualToken)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"Forbidden internal api\"}");
            return false;
        }

        return true;
    }
}