package com.xcvk.platform.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.xcvk.platform.auth.starter.constant.PlatformRoleConstants;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.knowledge.model.dto.KnowledgeChunkVectorSearchRequest;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkVectorSearchItemVO;
import com.xcvk.platform.knowledge.search.service.KnowledgeChunkVectorSearchService;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识文档切片向量检索控制器
 *
 * <p>用于承接 chunk 级向量检索入口，当前阶段基于用户问题生成 embedding，
 * 并在 Elasticsearch 的 kb_chunk 向量索引中检索语义相似的知识片段。</p>
 *
 * <p>该接口主要用于验证向量检索能力，后续可作为 AI / RAG 问答的知识召回基础。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-28
 */
@RestController
@RequestMapping("/chunks")
@RequiredArgsConstructor
public class KnowledgeChunkVectorSearchController {

    private final KnowledgeChunkVectorSearchService knowledgeChunkVectorSearchService;

    /**
     * 向量检索知识文档切片。
     *
     * @param request 向量检索请求
     * @return 相似知识片段列表
     */
    @PostMapping("/vector-search")
    @SaCheckLogin
    @SaCheckRole(
            value = {PlatformRoleConstants.ADMIN, PlatformRoleConstants.SUPPORT},
            mode = SaMode.OR
    )
    @AccessLog(value = "向量检索知识文档切片", recordArgs = false, recordResult = false)
    @Operation(summary = "向量检索知识文档切片", description = "基于用户问题 embedding 检索语义相似的知识文档切片")
    public Result<List<KnowledgeChunkVectorSearchItemVO>> vectorSearch(
            @Valid @RequestBody KnowledgeChunkVectorSearchRequest request) {
        return Result.success(knowledgeChunkVectorSearchService.vectorSearch(request));
    }
}