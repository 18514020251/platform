package com.xcvk.platform.ai.service;

import com.xcvk.platform.ai.model.dto.AssistantChatRequest;
import com.xcvk.platform.ai.model.vo.AssistantChatResponse;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;

/**
 * 智能助手服务。
 *
 * @author Programmer
 * @version 1.1
 * @date 2026-05-08
 */
public interface AssistantService {

    /**
     * 智能助手统一入口。
     *
     * @param identity 当前登录身份
     * @param request 用户请求
     * @return 助手响应
     */
    AssistantChatResponse chat(CurrentLoginIdentity identity, AssistantChatRequest request);
}