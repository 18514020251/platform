package com.xcvk.platform.ai.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.xcvk.platform.ai.model.dto.RagEvalDatasetCreateRequest;
import com.xcvk.platform.ai.model.dto.RagEvalRunRequest;
import com.xcvk.platform.ai.model.vo.RagEvalCaseResultVO;
import com.xcvk.platform.ai.model.vo.RagEvalDatasetVO;
import com.xcvk.platform.ai.model.vo.RagEvalRunVO;
import com.xcvk.platform.ai.service.RagEvalService;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RAG 离线评测控制器。
 */
@RestController
@RequestMapping("/rag/eval")
@RequiredArgsConstructor
public class RagEvalController {

    private final RagEvalService ragEvalService;

    @PostMapping("/datasets")
    @SaCheckLogin
    @AccessLog(value = "新增RAG评测样本", recordArgs = false, recordResult = false)
    @Operation(summary = "新增RAG评测样本", description = "新增 question-expectedChunk 标注样本")
    public Result<RagEvalDatasetVO> createDataset(@Valid @RequestBody RagEvalDatasetCreateRequest request) {
        return Result.success(ragEvalService.createDataset(request));
    }

    @GetMapping("/datasets")
    @SaCheckLogin
    @AccessLog(value = "查询RAG评测样本", recordArgs = false, recordResult = false)
    @Operation(summary = "查询RAG评测样本", description = "按分类查询RAG评测样本")
    public Result<List<RagEvalDatasetVO>> listDatasets(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return Result.success(ragEvalService.listDatasets(categoryId, limit));
    }

    @PostMapping("/runs")
    @SaCheckLogin
    @AccessLog(value = "执行RAG离线评测", recordArgs = false, recordResult = false)
    @Operation(summary = "执行RAG离线评测", description = "基于评测集计算Recall、MRR、Context Precision等指标")
    public Result<RagEvalRunVO> runEvaluation(@Valid @RequestBody(required = false) RagEvalRunRequest request) {
        return Result.success(ragEvalService.runEvaluation(request));
    }

    @GetMapping("/runs")
    @SaCheckLogin
    @AccessLog(value = "查询RAG评测任务", recordArgs = false, recordResult = false)
    @Operation(summary = "查询RAG评测任务", description = "查询历史RAG离线评测任务")
    public Result<List<RagEvalRunVO>> listRuns(
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return Result.success(ragEvalService.listRuns(limit));
    }

    @GetMapping("/runs/{runId}/cases")
    @SaCheckLogin
    @AccessLog(value = "查询RAG评测明细", recordArgs = false, recordResult = false)
    @Operation(summary = "查询RAG评测明细", description = "查询某次RAG评测的逐样本结果")
    public Result<List<RagEvalCaseResultVO>> listCaseResults(
            @PathVariable("runId") Long runId
    ) {
        return Result.success(ragEvalService.listCaseResults(runId));
    }
}