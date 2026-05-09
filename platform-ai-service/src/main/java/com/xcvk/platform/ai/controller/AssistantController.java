package com.xcvk.platform.ai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.xcvk.platform.ai.model.dto.AssistantChatRequest;
import com.xcvk.platform.ai.model.vo.AssistantChatResponse;
import com.xcvk.platform.ai.service.AssistantService;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.auth.starter.util.SaTokenSessionUtils;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 智能助手控制器。
 *
 * <p>统一承接企业知识问答、流程咨询和工单创建等自然语言请求。</p>
 *
 * @author Programmer
 * @version 1.2
 * @date 2026-05-08
 */
@RestController
@RequestMapping("/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;

    private final SaTokenSessionUtils saTokenSessionUtils;

    /**
     * 智能助手统一入口。
     *
     * <p>当前登录人信息统一从 Sa-Token Session 获取，
     * 不允许前端传入 creatorId / creatorName。</p>
     *
     * @param request 用户请求
     * @return 助手响应
     */
    @PostMapping("/chat")
    @SaCheckLogin
    @AccessLog(value = "智能助手对话", recordArgs = false, recordResult = false)
    @Operation(summary = "智能助手对话", description = "支持知识问答与AI工单创建")
    public Result<AssistantChatResponse> chat(@Valid @RequestBody AssistantChatRequest request) {
        CurrentLoginIdentity identity = saTokenSessionUtils.getCurrentLoginIdentity();
        AssistantChatResponse response = assistantService.chat(identity, request);
        return Result.success(response);
    }
}