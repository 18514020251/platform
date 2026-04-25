package com.xcvk.platform.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.auth.starter.util.SaTokenSessionUtils;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.knowledge.model.dto.CreateKnowledgeDocumentRequest;
import com.xcvk.platform.knowledge.model.dto.UpdateKnowledgeDocumentRequest;
import com.xcvk.platform.knowledge.service.KnowledgeDocumentService;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import com.xcvk.platform.auth.starter.constant.PlatformRoleConstants;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 知识文档控制器
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-24
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService knowledgeDocumentService;

    private final SaTokenSessionUtils saTokenSessionUtils;

    @PostMapping
    @SaCheckLogin
    @SaCheckRole(value = {PlatformRoleConstants.ADMIN, PlatformRoleConstants.SUPPORT}, mode = SaMode.OR)
    @AccessLog(value = "创建知识文档", recordArgs = false, recordResult = false)
    @Operation(summary = "创建知识文档", description = "创建知识库文档并同步写入 Elasticsearch")
    public Result<Long> createDocument(@Valid @RequestBody CreateKnowledgeDocumentRequest request) {
        CurrentLoginIdentity identity = saTokenSessionUtils.getCurrentLoginIdentity();
        return Result.success(knowledgeDocumentService.createDocument(identity, request));
    }

    @PutMapping("/{id}")
    @SaCheckLogin
    @SaCheckRole(value = {PlatformRoleConstants.ADMIN, PlatformRoleConstants.SUPPORT}, mode = SaMode.OR)
    @AccessLog(value = "更新知识文档", recordArgs = false, recordResult = false)
    @Operation(summary = "更新知识文档", description = "更新知识库文档并同步写入 Elasticsearch")
    public Result<Void> put(@PathVariable("id") Long id,
                            @Valid @RequestBody UpdateKnowledgeDocumentRequest request) {
        CurrentLoginIdentity identity = saTokenSessionUtils.getCurrentLoginIdentity();
        knowledgeDocumentService.updateDocument(identity, id, request);
        return Result.successVoid();
    }

    @PutMapping("/{documentId}/offline")
    @SaCheckLogin
    @SaCheckRole(value = {PlatformRoleConstants.ADMIN, PlatformRoleConstants.SUPPORT}, mode = SaMode.OR)
    @AccessLog(value = "下线知识文档", recordArgs = false, recordResult = false)
    @Operation(summary = "下线知识文档", description = "将知识文档状态更新为已下线，并同步 Elasticsearch")
    public Result<Void> offlineDocument(@PathVariable("documentId") Long documentId) {
        CurrentLoginIdentity identity = saTokenSessionUtils.getCurrentLoginIdentity();
        knowledgeDocumentService.offlineDocument(identity, documentId);
        return Result.successVoid();
    }
}