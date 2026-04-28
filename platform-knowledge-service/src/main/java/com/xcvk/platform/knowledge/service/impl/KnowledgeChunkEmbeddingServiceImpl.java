package com.xcvk.platform.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xcvk.platform.api.contract.ai.client.EmbedClient;
import com.xcvk.platform.api.contract.ai.model.EmbeddingRequest;
import com.xcvk.platform.api.contract.ai.model.EmbeddingResponse;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.knowledge.constant.KnowledgeChunkStatusConstants;
import com.xcvk.platform.knowledge.constant.KnowledgeErrorMessages;
import com.xcvk.platform.knowledge.model.entity.KnowledgeDocumentChunk;
import com.xcvk.platform.knowledge.service.KnowledgeChunkEmbeddingService;
import com.xcvk.platform.knowledge.service.KnowledgeDocumentChunkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 知识文档切片向量化服务实现类
 *
 * <p>当前阶段通过 platform-api-contract 中的 EmbedClient 调用 ai-service，
 * 将知识文档 chunk 文本转换为 embedding 向量。</p>
 *
 * <p>该类当前只负责远程调用和结果校验，不负责写入 ES dense_vector。
 * 等确认模型维度稳定后，再扩展 KnowledgeChunkIndex 的 embedding 字段。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-28
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeChunkEmbeddingServiceImpl implements KnowledgeChunkEmbeddingService {

    private final KnowledgeDocumentChunkService knowledgeDocumentChunkService;

    private final EmbedClient embedClient;

    /**
     * 为指定知识文档的 ACTIVE 切片生成 embedding。
     *
     * @param documentId 知识文档ID
     * @return 文本向量化响应
     */
    @Override
    public EmbeddingResponse embedDocumentChunks(Long documentId) {
        BizAssert.notNull(documentId, ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.DOCUMENT_ID_REQUIRED);

        List<KnowledgeDocumentChunk> chunks = listActiveChunks(documentId);
        BizAssert.isTrue(
                !CollectionUtils.isEmpty(chunks),
                ErrorCode.BIZ_ERROR,
                KnowledgeErrorMessages.DOCUMENT_CHUNK_NOT_FOUND
        );

        List<String> texts = chunks.stream()
                .map(KnowledgeDocumentChunk::getChunkText)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .toList();

        BizAssert.isTrue(
                !CollectionUtils.isEmpty(texts),
                ErrorCode.BIZ_ERROR,
                KnowledgeErrorMessages.DOCUMENT_CHUNK_TEXT_REQUIRED
        );

        EmbeddingResponse response = embedClient.embedTexts(new EmbeddingRequest(texts));
        validateEmbeddingResponse(response, texts.size());

        log.info(
                "知识文档切片向量化调用成功，documentId={}, chunkCount={}, modelName={}, dimension={}",
                documentId,
                texts.size(),
                response.modelName(),
                response.dimension()
        );

        return response;
    }

    /**
     * 查询指定文档的 ACTIVE 切片。
     *
     * @param documentId 知识文档ID
     * @return ACTIVE 切片列表
     */
    private List<KnowledgeDocumentChunk> listActiveChunks(Long documentId) {
        return knowledgeDocumentChunkService.list(
                new LambdaQueryWrapper<KnowledgeDocumentChunk>()
                        .eq(KnowledgeDocumentChunk::getDocumentId, documentId)
                        .eq(KnowledgeDocumentChunk::getStatus, KnowledgeChunkStatusConstants.ACTIVE)
                        .orderByAsc(KnowledgeDocumentChunk::getChunkNo)
        );
    }

    /**
     * 校验 AI 模块返回的 embedding 结果。
     *
     * @param response 文本向量化响应
     * @param expectedVectorCount 期望向量数量
     */
    private void validateEmbeddingResponse(EmbeddingResponse response, int expectedVectorCount) {
        BizAssert.notNull(response, ErrorCode.BIZ_ERROR, KnowledgeErrorMessages.EMBEDDING_RESPONSE_INVALID);

        BizAssert.hasText(response.modelName(), ErrorCode.BIZ_ERROR, KnowledgeErrorMessages.EMBEDDING_MODEL_REQUIRED);

        BizAssert.notNull(response.dimension(), ErrorCode.BIZ_ERROR, KnowledgeErrorMessages.EMBEDDING_DIMENSION_REQUIRED);
        BizAssert.isTrue(response.dimension() > 0, ErrorCode.BIZ_ERROR, KnowledgeErrorMessages.EMBEDDING_DIMENSION_INVALID);

        BizAssert.isTrue(
                !CollectionUtils.isEmpty(response.vectors()),
                ErrorCode.BIZ_ERROR,
                KnowledgeErrorMessages.EMBEDDING_VECTOR_REQUIRED
        );

        BizAssert.isTrue(
                response.vectors().size() == expectedVectorCount,
                ErrorCode.BIZ_ERROR,
                KnowledgeErrorMessages.EMBEDDING_VECTOR_COUNT_NOT_MATCH
        );

        boolean allDimensionMatched = response.vectors().stream()
                .allMatch(vector -> vector != null && vector.size() == response.dimension());

        BizAssert.isTrue(
                allDimensionMatched,
                ErrorCode.BIZ_ERROR,
                KnowledgeErrorMessages.EMBEDDING_VECTOR_DIMENSION_NOT_MATCH
        );
    }
}