package com.xcvk.platform.knowledge.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xcvk.platform.auth.starter.constant.PlatformRoleConstants;
import com.xcvk.platform.common.domain.Result;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.knowledge.constant.KnowledgeErrorMessages;
import com.xcvk.platform.knowledge.model.entity.KnowledgeDocumentChunk;
import com.xcvk.platform.knowledge.model.vo.KnowledgeDocumentChunkItemVO;
import com.xcvk.platform.knowledge.service.KnowledgeDocumentChunkService;
import com.xcvk.platform.log.starter.annotation.AccessLog;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识文档切片控制器
 *
 * <p>当前阶段主要用于开发调试，方便查看指定知识文档生成的 chunk 列表。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-26
 */
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class KnowledgeDocumentChunkController {

    private final KnowledgeDocumentChunkService knowledgeDocumentChunkService;

    /**
     * 查询指定知识文档的切片列表。
     *
     * <p>该接口用于开发阶段验证切片结果，后续接入 embedding 和向量索引时，
     * 也可以用于排查某个 chunk 是否正确生成。</p>
     *
     * @param documentId 知识文档ID
     * @return 知识文档切片列表
     */
    @GetMapping("/{documentId}/chunks/test")
    @SaCheckLogin
    @SaCheckRole(
            value = {PlatformRoleConstants.ADMIN, PlatformRoleConstants.SUPPORT},
            mode = SaMode.OR
    )
    @AccessLog(value = "查询知识文档切片列表", recordArgs = false, recordResult = false)
    @Operation(summary = "查询知识文档切片列表", description = "查询指定知识文档自动生成的切片列表")
    public Result<List<KnowledgeDocumentChunkItemVO>> listDocumentChunks(@PathVariable("documentId") Long documentId) {
        BizAssert.notNull(documentId, ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.DOCUMENT_ID_REQUIRED);

        List<KnowledgeDocumentChunk> chunks = knowledgeDocumentChunkService.list(
                new LambdaQueryWrapper<KnowledgeDocumentChunk>()
                        .eq(KnowledgeDocumentChunk::getDocumentId, documentId)
                        .orderByAsc(KnowledgeDocumentChunk::getChunkNo)
        );

        List<KnowledgeDocumentChunkItemVO> records = chunks.stream()
                .map(this::toChunkItemVO)
                .toList();

        return Result.success(records);
    }

    /**
     * 将知识文档切片实体转换为列表项。
     *
     * @param chunk 知识文档切片实体
     * @return 知识文档切片列表项
     */
    private KnowledgeDocumentChunkItemVO toChunkItemVO(KnowledgeDocumentChunk chunk) {
        return new KnowledgeDocumentChunkItemVO(
                chunk.getId(),
                chunk.getDocumentId(),
                chunk.getChunkNo(),
                chunk.getChunkText(),
                chunk.getChunkHash(),
                chunk.getTokenCount(),
                chunk.getStatus(),
                chunk.getCreatedAt(),
                chunk.getUpdatedAt()
        );
    }
}