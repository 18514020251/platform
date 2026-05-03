package com.xcvk.platform.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.xcvk.platform.auth.starter.constant.PlatformRoleConstants;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.knowledge.model.dto.KnowledgeChunkHybridSearchRequest;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkHybridSearchItemVO;
import com.xcvk.platform.knowledge.search.service.KnowledgeChunkHybridSearchService;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识文档切片混合检索控制器
 *
 * <p>用于承接 chunk 级混合检索入口，融合全文检索和向量检索结果，
 * 为后续 AI / RAG 问答提供更稳定的知识片段召回能力。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
@RestController
@RequestMapping("/chunks")
@RequiredArgsConstructor
public class KnowledgeChunkHybridSearchController {

    private final KnowledgeChunkHybridSearchService knowledgeChunkHybridSearchService;

    /**
     * 混合检索知识文档切片。
     *
     * <p>当前阶段会同时执行 chunk 全文检索和向量检索，
     * 并对两路结果进行简单融合排序。</p>
     *
     * @param request 混合检索请求
     * @return 融合后的知识片段列表
     */
    @PostMapping("/hybrid-search")
    @SaCheckLogin
    @SaCheckRole(
            value = {PlatformRoleConstants.ADMIN, PlatformRoleConstants.SUPPORT},
            mode = SaMode.OR
    )
    @AccessLog(value = "混合检索知识文档切片", recordArgs = false, recordResult = false)
    @Operation(summary = "混合检索知识文档切片", description = "融合全文检索和向量检索结果，返回相关知识文档切片")
    public Result<List<KnowledgeChunkHybridSearchItemVO>> hybridSearch(
            @Valid @RequestBody KnowledgeChunkHybridSearchRequest request) {
        return Result.success(knowledgeChunkHybridSearchService.hybridSearch(request));
    }
}