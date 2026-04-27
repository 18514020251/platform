package com.xcvk.platform.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.xcvk.platform.auth.starter.constant.PlatformRoleConstants;
import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.knowledge.model.query.KnowledgeChunkSearchQuery;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkSearchItemVO;
import com.xcvk.platform.knowledge.search.service.KnowledgeChunkSearchService;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识文档切片搜索控制器
 *
 * <p>用于承接 chunk 级全文检索入口，当前阶段先基于 Elasticsearch
 * 实现知识片段搜索，为后续 AI / RAG 检索做准备。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-27
 */
@RestController
@RequestMapping("/chunks")
@RequiredArgsConstructor
public class KnowledgeChunkSearchController {

    private final KnowledgeChunkSearchService knowledgeChunkSearchService;

    /**
     * 搜索知识文档切片。
     *
     * <p>该接口当前主要用于开发验证和内部调试，
     * 后续可作为 AI / RAG 查询知识片段的基础检索能力。</p>
     *
     * @param query chunk 搜索查询条件
     * @return chunk 搜索分页结果
     */
    @GetMapping("/search")
    @SaCheckLogin
    @SaCheckRole(
            value = {PlatformRoleConstants.ADMIN, PlatformRoleConstants.SUPPORT},
            mode = SaMode.OR
    )
    @AccessLog(value = "ES 搜索知识文档切片列表", recordArgs = false, recordResult = false)
    @Operation(summary = "ES 搜索知识文档切片列表", description = "基于 Elasticsearch 搜索知识文档切片")
    public Result<PageResult<KnowledgeChunkSearchItemVO>> searchChunks(
            @ModelAttribute KnowledgeChunkSearchQuery query) {
        return Result.success(knowledgeChunkSearchService.searchChunks(query));
    }
}