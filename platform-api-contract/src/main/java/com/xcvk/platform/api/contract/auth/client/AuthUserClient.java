package com.xcvk.platform.api.contract.auth.client;

import com.xcvk.platform.api.contract.auth.model.InternalUserInfoResponse;
import com.xcvk.platform.common.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 认证服务内部远程调用接口。
 *
 * <p>用于 workflow-service 查询用户是否存在、是否启用、是否具备处理工单权限。</p>
 */
@FeignClient(
        name = "platform-auth",
        contextId = "platformAuthUserClient",
        url = "${platform.remote.auth-base-url}"
)
public interface AuthUserClient {

    /**
     * 根据用户ID查询内部用户信息。
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    @GetMapping("/internal/users/{userId}")
    Result<InternalUserInfoResponse> getUserById(@PathVariable("userId") Long userId);
}