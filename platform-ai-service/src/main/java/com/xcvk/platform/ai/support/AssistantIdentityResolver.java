package com.xcvk.platform.ai.support;

import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Assistant 登录身份解析器。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-09
 */
@Component
public class AssistantIdentityResolver {

    /**
     * 解析当前登录人名称。
     *
     * @param identity 当前登录身份
     * @return 用户名称
     */
    public String resolveCreatorName(CurrentLoginIdentity identity) {
        if (identity == null) {
            return "未知用户";
        }

        if (StringUtils.hasText(identity.realName())) {
            return identity.realName().trim();
        }

        if (StringUtils.hasText(identity.username())) {
            return identity.username().trim();
        }

        return "用户" + identity.userId();
    }
}