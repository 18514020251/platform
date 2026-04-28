package com.xcvk.platform.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.xcvk.platform.api.contract.ai.model.EmbeddingResponse;
import com.xcvk.platform.auth.starter.constant.PlatformRoleConstants;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkEmbeddingResultVO;
import com.xcvk.platform.knowledge.service.KnowledgeChunkEmbeddingService;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 知识文档切片向量化控制器
 *
 * <p>当前阶段主要用于开发调试，手动触发某篇知识文档的 chunk 向量化，
 * 验证 knowledge-service 调用 ai-service 的 embedding 链路是否正常。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-28
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class KnowledgeChunkEmbeddingController {

    private final KnowledgeChunkEmbeddingService knowledgeChunkEmbeddingService;

    /**
     * 重建知识文档切片向量。
     *
     * <p>当前阶段只调用 AI 模块生成 embedding 并返回摘要信息，
     * 暂不写入 Elasticsearch dense_vector 字段。</p>
     *
     * @param documentId 知识文档ID
     * @return 向量化结果摘要
     */
    @PostMapping("/{documentId}/embeddings/rebuild")
    @SaCheckLogin
    @SaCheckRole(
            value = {PlatformRoleConstants.ADMIN, PlatformRoleConstants.SUPPORT},
            mode = SaMode.OR
    )
    @AccessLog(value = "重建知识文档切片向量", recordArgs = false, recordResult = false)
    @Operation(summary = "重建知识文档切片向量", description = "调用 AI 模块为知识文档切片生成 embedding")
    public Result<KnowledgeChunkEmbeddingResultVO> rebuildDocumentChunkEmbeddings(@PathVariable("documentId") Long documentId) {
        EmbeddingResponse response = knowledgeChunkEmbeddingService.embedDocumentChunks(documentId);

        return Result.success(new KnowledgeChunkEmbeddingResultVO(
                documentId,
                response.modelName(),
                response.dimension(),
                response.vectors().size()
        ));
    }
}