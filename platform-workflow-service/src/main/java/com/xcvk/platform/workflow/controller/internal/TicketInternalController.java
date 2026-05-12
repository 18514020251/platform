package com.xcvk.platform.workflow.controller.internal;

import com.xcvk.platform.api.contract.workflow.model.CreateAiTicketRequest;
import com.xcvk.platform.api.contract.workflow.model.CreateAiTicketResponse;
import com.xcvk.platform.api.contract.workflow.model.QueryAiTicketRequest;
import com.xcvk.platform.api.contract.workflow.model.QueryAiTicketResponse;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.workflow.model.vo.CreateTicketResponse;
import com.xcvk.platform.workflow.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 工单内部接口。
 *
 * <p>该接口面向服务间调用，不直接暴露给前端。
 * 当前主要用于 ai-service 通过 Agent Tool 创建工单。</p>
 *
 * @author Programmer
 * @version 1.1
 * @date 2026-05-08
 */
@RestController
@RequestMapping("/internal/ai/tickets")
@RequiredArgsConstructor
public class TicketInternalController {

    private final TicketService ticketService;

    /**
     * AI Agent 创建工单。
     *
     * @param request AI 创建工单请求
     * @return 创建结果
     */
    @PostMapping
    public Result<CreateAiTicketResponse> createTicketByAi(@Valid @RequestBody CreateAiTicketRequest request) {
        CreateTicketResponse response = ticketService.createAiTicket(
                request.creatorId(),
                request.creatorName(),
                request.ticketTypeCode(),
                request.title(),
                request.content(),
                request.priority(),
                request.sourceRef()
        );

        return Result.success(new CreateAiTicketResponse(
                response.ticketId(),
                response.ticketNo(),
                response.status()
        ));
    }

    /**
     * AI Agent 查询当前用户工单。
     *
     * @param request AI 工单查询请求
     * @return 工单查询结果
     */
    @PostMapping("/query")
    public Result<QueryAiTicketResponse> queryTicketByAi(@RequestBody QueryAiTicketRequest request) {
        return Result.success(ticketService.queryTicketByAi(request));
    }
}