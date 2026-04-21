package com.xcvk.platform.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.auth.starter.util.SaTokenSessionUtils;
import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import com.xcvk.platform.workflow.constant.TicketSourceConstants;
import com.xcvk.platform.workflow.model.cmd.CreateTicketCmd;
import com.xcvk.platform.workflow.model.dto.CreateTicketRequest;
import com.xcvk.platform.workflow.model.query.MyTicketQuery;
import com.xcvk.platform.workflow.model.vo.CreateTicketResponse;
import com.xcvk.platform.workflow.model.vo.TicketDetailVO;
import com.xcvk.platform.workflow.model.vo.TicketListItemVO;
import com.xcvk.platform.workflow.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 工单控制器
 *
 * <p>当前阶段先提供员工主链相关接口：
 * 创建工单、我的工单列表、工单详情。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
@RestController
@RequestMapping("/ticket")
@RequiredArgsConstructor
@Validated
@Tag(name = "工单管理", description = "工单创建、查询与详情")
public class TicketController {

    private final TicketService ticketService;
    private final SaTokenSessionUtils saTokenSessionUtils;

    /**
     * 创建工单
     *
     * <p>手工创建工单时，创建人信息统一来自当前登录身份，
     * 不允许由前端直接传入创建人字段。</p>
     *
     * @param request 创建工单请求
     * @return 创建结果
     */
    @PostMapping
    @SaCheckLogin
    @AccessLog(value = "创建工单", recordArgs = true, recordResult = false)
    @Operation(summary = "创建工单", description = "手工创建工单")
    public Result<CreateTicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        CurrentLoginIdentity identity = saTokenSessionUtils.getCurrentLoginIdentity();

        CreateTicketCmd cmd = new CreateTicketCmd(
                identity.userId(),
                identity.realName(),
                request.ticketTypeCode(),
                request.title(),
                request.content(),
                request.priority(),
                TicketSourceConstants.MANUAL,
                null
        );

        return Result.success(ticketService.createTicket(cmd));
    }

    /**
     * 我的工单列表
     *
     * @param query 查询条件
     * @return 我的工单分页结果
     */
    @GetMapping("/my")
    @SaCheckLogin
    @AccessLog(value = "查询我的工单列表", recordArgs = false, recordResult = false)
    @Operation(summary = "我的工单列表", description = "分页查询当前登录用户创建的工单")
    public Result<PageResult<TicketListItemVO>> pageMyTickets(@ModelAttribute MyTicketQuery query) {
        Long currentUserId = saTokenSessionUtils.getCurrentLoginIdentity().userId();
        return Result.success(ticketService.pageMyTickets(currentUserId, query));
    }

    /**
     * 我的工单详情
     *
     * @param ticketId 工单ID
     * @return 工单详情
     */
    @GetMapping("/my/{ticketId}")
    @SaCheckLogin
    @AccessLog(value = "查询我的工单详情", recordArgs = false, recordResult = false)
    @Operation(summary = "我的工单详情", description = "查询当前登录用户自己的工单详情")
    public Result<TicketDetailVO> getMyTicketDetail(@PathVariable("ticketId") Long ticketId) {
        Long currentUserId = saTokenSessionUtils.getCurrentLoginIdentity().userId();
        return Result.success(ticketService.getMyTicketDetail(currentUserId, ticketId));
    }
}