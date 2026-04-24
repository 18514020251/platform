package com.xcvk.platform.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.knowledge.model.query.KnowledgeDocumentSearchQuery;
import com.xcvk.platform.knowledge.model.vo.KnowledgeDocumentSearchItemVO;
import com.xcvk.platform.knowledge.search.service.KnowledgeSearchService;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识文档搜索控制器
 *
 * <p>用于承接知识库全文检索入口，当前阶段先基于 Elasticsearch
 * 实现文档级 title、summary、content 搜索。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-24
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class KnowledgeSearchController {

    private final KnowledgeSearchService knowledgeSearchService;

    /**
     * 搜索知识文档。
     *
     * @param query 搜索查询条件
     * @return 知识文档分页结果
     */
    @GetMapping("/search")
    @SaCheckLogin
    @AccessLog(value = "ES 搜索知识文档列表", recordArgs = false, recordResult = false)
    @Operation(summary = "ES 搜索知识文档列表", description = "基于 Elasticsearch 搜索知识库文档")
    public Result<PageResult<KnowledgeDocumentSearchItemVO>> searchDocuments(
            @ModelAttribute KnowledgeDocumentSearchQuery query) {
        return Result.success(knowledgeSearchService.searchDocuments(query));
    }
}