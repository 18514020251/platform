package com.xcvk.platform.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.xcvk.platform.auth.starter.constant.PlatformRoleConstants;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.auth.starter.util.SaTokenSessionUtils;
import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import com.xcvk.platform.workflow.constant.TicketSourceConstants;
import com.xcvk.platform.workflow.model.cmd.CreateTicketCmd;
import com.xcvk.platform.workflow.model.dto.AssignTicketRequest;
import com.xcvk.platform.workflow.model.dto.CreateTicketRequest;
import com.xcvk.platform.workflow.model.dto.UpdateTicketStatusRequest;
import com.xcvk.platform.workflow.model.query.MyTicketQuery;
import com.xcvk.platform.workflow.model.query.TicketManageQuery;
import com.xcvk.platform.workflow.model.vo.CreateTicketResponse;
import com.xcvk.platform.workflow.model.vo.TicketDetailVO;
import com.xcvk.platform.workflow.model.vo.TicketListItemVO;
import com.xcvk.platform.workflow.model.vo.TicketManageListItemVO;
import com.xcvk.platform.workflow.search.service.TicketSearchService;
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
 * <p>当前阶段对外提供两类工单接口：</p>
 * <ul>
 *     <li>员工侧：创建工单、我的工单列表、我的工单详情</li>
 *     <li>处理侧：支持人员/管理员视角的工单列表、接单</li>
 * </ul>
 *
 * <p>控制器层只负责接参与返回结果，
 * 具体业务规则和数据范围收敛到 Service 层实现，
 * 以保证控制器职责单一、主流程清晰。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
@Validated
@Tag(name = "工单管理", description = "工单创建、查询与处理")
public class TicketController {

    private final TicketService ticketService;
    private final TicketSearchService ticketSearchService;
    private final SaTokenSessionUtils saTokenSessionUtils;
    /**
     * 创建工单
     *
     * <p>手工创建工单时，创建人信息统一来自当前登录身份，
     * 不允许由前端直接传入创建人字段，
     * 以避免伪造创建人导致的数据越权或审计混乱。</p>
     *
     * @param request 创建工单请求
     * @return 创建结果
     */
    // TODO cmd 组装放cmd
    @PostMapping
    @SaCheckLogin
    @AccessLog(value = "创建工单", recordArgs = true, recordResult = false)
    @Operation(summary = "创建工单", description = "手工创建工单")
    public Result<CreateTicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request) {
        CurrentLoginIdentity identity = saTokenSessionUtils.getCurrentLoginIdentity();

        return Result.success(ticketService.createTicket(
                identity.userId(),
                identity.realName(),
                request.ticketTypeCode(),
                request.title(),
                request.content(),
                request.priority()
        ));
    }

    /**
     * 我的工单列表
     *
     * <p>该接口只返回当前登录用户本人创建的工单，
     * 不提供跨人查询能力，避免员工侧列表被误用为处理侧列表入口。</p>
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
     * <p>该接口只允许查看当前登录用户自己的工单详情，
     * 防止用户通过猜测工单ID访问他人工单。</p>
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

    /**
     * 处理侧工单列表
     *
     * <p>该接口面向支持人员与管理员使用，
     * 用于进入处理工作台后查看当前可处理的工单集合。</p>
     *
     * <p>这里先通过角色注解做接口准入控制，
     * 避免无关角色进入处理侧入口；
     * 进入接口后的具体数据范围，仍由 Service 层继续收口控制。</p>
     *
     * @param query 查询条件
     * @return 处理侧工单分页结果
     */
    @GetMapping
    @SaCheckLogin
    @SaCheckRole(
            value = {PlatformRoleConstants.ADMIN, PlatformRoleConstants.SUPPORT},
            mode = SaMode.OR
    )
    @AccessLog(value = "查询处理侧工单列表", recordArgs = false, recordResult = false)
    @Operation(summary = "处理侧工单列表", description = "支持人员或管理员分页查询可处理工单")
    public Result<PageResult<TicketManageListItemVO>> pageManageTickets(@ModelAttribute TicketManageQuery query) {
        CurrentLoginIdentity identity = saTokenSessionUtils.getCurrentLoginIdentity();
        return Result.success(ticketService.pageManageTickets(identity, query));
    }

    /**
     * 接单
     *
     * <p>该接口面向支持人员与管理员使用，
     * 用于将“待受理且未分派”的工单认领为当前处理人。</p>
     *
     * <p>接口入口先通过角色注解做准入控制，
     * 具体是否允许接单、是否存在并发抢单等问题，
     * 统一由 Service 层处理。</p>
     *
     * @param ticketId 工单ID
     * @return 成功响应
     */
    @PostMapping("/{ticketId}/accept")
    @SaCheckLogin
    @SaCheckRole(
            value = {PlatformRoleConstants.ADMIN, PlatformRoleConstants.SUPPORT},
            mode = SaMode.OR
    )
    @AccessLog(value = "工单接单", recordArgs = false, recordResult = false)
    @Operation(summary = "工单接单", description = "支持人员或管理员接单")
    public Result<Void> acceptTicket(@PathVariable("ticketId") Long ticketId) {
        CurrentLoginIdentity identity = saTokenSessionUtils.getCurrentLoginIdentity();
        ticketService.acceptTicket(identity, ticketId);
        return Result.successVoid();
    }

    /**
     * 更新工单状态
     *
     * <p>该接口面向支持人员与管理员使用，
     * 用于将当前正在处理的工单更新为已解决或已拒绝。</p>
     *
     * <p>接口入口先通过角色注解做准入控制，
     * 具体是否允许操作该工单、当前状态是否允许流转，
     * 统一由 Service 层继续处理。</p>
     *
     * @param ticketId 工单ID
     * @param request 更新状态请求
     * @return 成功响应
     */
    @PutMapping("/{ticketId}/status")
    @SaCheckLogin
    @SaCheckRole(
            value = {PlatformRoleConstants.ADMIN, PlatformRoleConstants.SUPPORT},
            mode = SaMode.OR
    )
    @AccessLog(value = "更新工单状态", recordArgs = true, recordResult = false)
    @Operation(summary = "更新工单状态", description = "支持人员或管理员更新工单状态")
    public Result<Void> updateTicketStatus(@PathVariable("ticketId") Long ticketId,
                                           @Valid @RequestBody UpdateTicketStatusRequest request) {
        CurrentLoginIdentity identity = saTokenSessionUtils.getCurrentLoginIdentity();
        ticketService.updateTicketStatus(identity, ticketId, request);
        return Result.successVoid();
    }

    @PostMapping("/manage/{ticketId}/assign")
    @SaCheckLogin
    @SaCheckRole(PlatformRoleConstants.ADMIN)
    @AccessLog(value = "工单分配", recordArgs = true, recordResult = false)
    @Operation(summary = "工单分配", description = "管理员分配工单")
    public Result<Void> assignTicket(
            @PathVariable("ticketId") Long ticketId,
            @RequestBody AssignTicketRequest request
    ) {
        ticketService.assignTicket(saTokenSessionUtils.getCurrentLoginIdentity(), ticketId, request);
        return Result.successVoid();
    }

    @GetMapping("/search")
    @SaCheckLogin
    @SaCheckRole(
            value = {PlatformRoleConstants.ADMIN, PlatformRoleConstants.SUPPORT},
            mode = SaMode.OR
    )
    @AccessLog(value = "ES 搜索处理侧工单列表", recordArgs = false, recordResult = false)
    @Operation(summary = "ES 搜索处理侧工单列表", description = "基于 Elasticsearch 搜索支持人员或管理员可处理的工单")
    public Result<PageResult<TicketManageListItemVO>> searchManageTickets(@ModelAttribute TicketManageQuery query) {
        CurrentLoginIdentity identity = saTokenSessionUtils.getCurrentLoginIdentity();
        return Result.success(ticketSearchService.searchManageTickets(identity, query));
    }
}