package com.xcvk.platform.log.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.xcvk.platform.auth.starter.constant.PlatformRoleConstants;
import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.log.model.query.AccessLogPageQuery;
import com.xcvk.platform.log.model.vo.AccessLogItemVO;
import com.xcvk.platform.log.service.AccessLogQueryService;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 接口访问日志查询控制器。
 *
 * <p>访问日志包含接口参数摘要、用户和错误信息，只允许系统管理员查看。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-11
 */
@RestController
@RequestMapping("/access-logs")
@RequiredArgsConstructor
public class AccessLogQueryController {

    private final AccessLogQueryService accessLogQueryService;

    @GetMapping
    @SaCheckLogin
    @SaCheckRole(PlatformRoleConstants.ADMIN)
    @AccessLog(value = "分页查询接口访问日志", recordArgs = false, recordResult = false)
    @Operation(summary = "分页查询接口访问日志")
    public Result<PageResult<AccessLogItemVO>> pageAccessLogs(
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") long pageSize,
            @RequestParam(value = "traceId", required = false) String traceId,
            @RequestParam(value = "serviceName", required = false) String serviceName,
            @RequestParam(value = "url", required = false) String url,
            @RequestParam(value = "userId", required = false) String userId,
            @RequestParam(value = "success", required = false) Boolean success,
            @RequestParam(value = "startTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        AccessLogPageQuery query = new AccessLogPageQuery(
                pageNum,
                pageSize,
                traceId,
                serviceName,
                url,
                userId,
                success,
                startTime,
                endTime
        );
        return Result.success(accessLogQueryService.pageAccessLogs(query));
    }

    @GetMapping("/trace/{traceId}")
    @SaCheckLogin
    @SaCheckRole(PlatformRoleConstants.ADMIN)
    @AccessLog(value = "按traceId查询接口访问日志", recordArgs = false, recordResult = false)
    @Operation(summary = "按 traceId 查询接口访问日志")
    public Result<List<AccessLogItemVO>> listByTraceId(@PathVariable("traceId") String traceId) {
        return Result.success(accessLogQueryService.listByTraceId(traceId));
    }
}
