package com.xcvk.platform.api.contract.workflow.client;

import com.xcvk.platform.api.contract.workflow.model.CreateAiTicketRequest;
import com.xcvk.platform.api.contract.workflow.model.CreateAiTicketResponse;
import com.xcvk.platform.common.domain.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 工单服务远程调用接口。
 *
 * <p>用于 ai-service 通过受控 Tool 调用 workflow-service。</p>
 *
 * @author Programmer
 * @version 1.1
 * @date 2026-05-08
 */
@FeignClient(
        name = "platform-workflow",
        contextId = "platformWorkflowTicketClient",
        url = "${platform.remote.workflow-base-url}"
)
public interface WorkflowTicketClient {

    /**
     * AI Agent 创建工单。
     *
     * <p>workflow-service 控制器统一返回 Result 包装结构，
     * 因此 Feign 侧也必须接收 Result，否则会导致 data 内字段无法映射。</p>
     *
     * @param request AI 创建工单请求
     * @return 创建结果
     */
    @PostMapping("/internal/ai/tickets")
    Result<CreateAiTicketResponse> createTicketByAi(@RequestBody CreateAiTicketRequest request);
}