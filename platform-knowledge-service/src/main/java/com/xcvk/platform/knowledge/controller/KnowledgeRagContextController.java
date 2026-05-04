package com.xcvk.platform.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.xcvk.platform.auth.starter.constant.PlatformRoleConstants;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.knowledge.model.dto.KnowledgeChunkHybridSearchRequest;
import com.xcvk.platform.knowledge.model.vo.KnowledgeRagContextItemVO;
import com.xcvk.platform.knowledge.service.KnowledgeRagContextService;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * RAG 知识上下文控制器
 *
 * <p>当前阶段主要用于开发调试，验证混合检索结果能否转换为
 * 适合大模型 Prompt 使用的干净知识片段。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class KnowledgeRagContextController {

    private final KnowledgeRagContextService knowledgeRagContextService;

    /**
     * 召回 RAG 知识上下文。
     *
     * <p>该接口会复用 chunk 混合检索能力，并清理前端展示用的高亮标签，
     * 返回可用于大模型 Prompt 的知识片段。</p>
     *
     * @param request 混合检索请求
     * @return RAG 知识上下文片段列表
     */
    @PostMapping("/contexts")
    @SaCheckLogin
    @SaCheckRole(
            value = {PlatformRoleConstants.ADMIN, PlatformRoleConstants.SUPPORT},
            mode = SaMode.OR
    )
    @AccessLog(value = "召回RAG知识上下文", recordArgs = false, recordResult = false)
    @Operation(summary = "召回RAG知识上下文", description = "基于混合检索召回适合大模型使用的知识片段")
    public Result<List<KnowledgeRagContextItemVO>> retrieveContexts(
            @Valid @RequestBody KnowledgeChunkHybridSearchRequest request) {
        return Result.success(knowledgeRagContextService.retrieveContexts(request));
    }
}