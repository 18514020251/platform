package com.xcvk.platform.workflow.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import com.xcvk.platform.workflow.model.vo.TicketTypeOptionVO;
import com.xcvk.platform.workflow.service.TicketTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工单类型控制器
 *
 * <p>当前阶段只提供启用状态的工单类型选项查询，
 * 供前端创建工单和后续 AI 模块识别类型时复用。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
@RestController
@RequestMapping("/types")
@RequiredArgsConstructor
@Tag(name = "工单类型管理", description = "工单类型选项查询")
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    /**
     * 查询启用状态的工单类型选项
     *
     * @return 工单类型选项列表
     */
    @GetMapping("/enabled-options")
    @SaCheckLogin
    @AccessLog(value = "查询启用工单类型选项", recordArgs = false, recordResult = false)
    @Operation(summary = "查询启用工单类型选项", description = "返回当前可用的工单类型列表")
    public Result<List<TicketTypeOptionVO>> listEnabledOptions() {
        return Result.success(ticketTypeService.listEnabledOptions());
    }
}