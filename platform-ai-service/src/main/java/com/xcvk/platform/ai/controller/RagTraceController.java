package com.xcvk.platform.ai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.xcvk.platform.ai.model.vo.trace.RagTraceTimelineVO;
import com.xcvk.platform.ai.trace.service.RagTraceService;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 *  RAG Trace 追踪控制器。
 * */
@RestController
@RequestMapping("/trace")
@RequiredArgsConstructor
public class RagTraceController {

    private final RagTraceService ragTraceService;

    @GetMapping("/execution-log/{id}")
    @Operation(summary = "查询 AI Trace Timeline")
    @SaCheckLogin
    @AccessLog(value = "查询 AI Trace Timeline", recordArgs = false, recordResult = false)
    public Result<RagTraceTimelineVO> getTimeline(
            @PathVariable("id") Long executionLogId
    ) {

        return Result.success(
                ragTraceService.getTimeline(executionLogId)
        );
    }
}