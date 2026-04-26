package com.xcvk.platform.knowledge.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.common.util.DbAssert;
import com.xcvk.platform.knowledge.assembler.KnowledgeDocumentChunkAssembler;
import com.xcvk.platform.knowledge.constant.KnowledgeChunkStatusConstants;
import com.xcvk.platform.knowledge.constant.KnowledgeErrorMessages;
import com.xcvk.platform.knowledge.model.entity.KnowledgeDocument;
import com.xcvk.platform.knowledge.model.entity.KnowledgeDocumentChunk;
import com.xcvk.platform.knowledge.repository.mapper.KnowledgeDocumentChunkMapper;
import com.xcvk.platform.knowledge.service.KnowledgeDocumentChunkService;
import com.xcvk.platform.knowledge.support.KnowledgeChunkSplitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识文档切片服务实现类
 *
 * <p>当前阶段只负责维护 MySQL 中的知识文档切片数据，
 * 不负责生成 embedding，也不负责写入 Elasticsearch 向量索引。</p>
 *
 * <p>切片数据是后续向量化、语义检索和 RAG 的中间数据基础。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-26
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeDocumentChunkServiceImpl
        extends ServiceImpl<KnowledgeDocumentChunkMapper, KnowledgeDocumentChunk>
        implements KnowledgeDocumentChunkService {

    private final KnowledgeChunkSplitter knowledgeChunkSplitter;

    private final KnowledgeDocumentChunkAssembler knowledgeDocumentChunkAssembler;

    /**
     * 重建知识文档切片。
     *
     * <p>当前阶段采用简单策略：先删除旧切片，再根据最新正文重新生成新切片。
     * 这种方式实现简单、结果确定，适合第一版最小闭环。</p>
     *
     * <p>后续如果需要优化性能，可以基于 chunkHash 做差异对比，
     * 只更新发生变化的切片。</p>
     *
     * @param document 知识文档实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildDocumentChunks(KnowledgeDocument document) {
        validateDocument(document);

        removeOldChunks(document.getId());

        List<String> chunkTexts = knowledgeChunkSplitter.split(document.getContent());
        List<KnowledgeDocumentChunk> chunks = knowledgeDocumentChunkAssembler.toChunks(document.getId(), chunkTexts);

        if (CollectionUtils.isEmpty(chunks)) {
            log.warn("知识文档未生成有效切片，documentId={}", document.getId());
            return;
        }

        saveChunks(chunks);

        log.info("重建知识文档切片成功，documentId={}, chunkCount={}", document.getId(), chunks.size());
    }

    private void saveChunks(List<KnowledgeDocumentChunk> chunks) {
        if (CollectionUtils.isEmpty(chunks)) {
            return;
        }

        for (KnowledgeDocumentChunk chunk : chunks) {
            int rows = baseMapper.insert(chunk);
            DbAssert.affectedOne(rows, KnowledgeErrorMessages.SAVE_DOCUMENT_CHUNK_FAILED);
        }
    }

    /**
     * 下线知识文档切片。
     *
     * <p>下线文档时，chunk 作为派生检索数据也需要同步下线，
     * 避免后续向量检索或混合检索召回已下线内容。</p>
     *
     * @param documentId 知识文档ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineDocumentChunks(Long documentId) {
        BizAssert.notNull(documentId, ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.DOCUMENT_ID_REQUIRED);

        KnowledgeDocumentChunk updateEntity = new KnowledgeDocumentChunk()
                .setStatus(KnowledgeChunkStatusConstants.OFFLINE)
                .setUpdatedAt(LocalDateTime.now());

        LambdaUpdateWrapper<KnowledgeDocumentChunk> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(KnowledgeDocumentChunk::getDocumentId, documentId)
                .eq(KnowledgeDocumentChunk::getStatus, KnowledgeChunkStatusConstants.ACTIVE);

        int rows = baseMapper.update(updateEntity, updateWrapper);

        log.info("下线知识文档切片完成，documentId={}, affectedRows={}", documentId, rows);
    }

    /**
     * 删除指定文档的旧切片。
     *
     * @param documentId 知识文档ID
     */
    private void removeOldChunks(Long documentId) {
        LambdaQueryWrapper<KnowledgeDocumentChunk> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(KnowledgeDocumentChunk::getDocumentId, documentId);

        boolean removed = this.remove(queryWrapper);
        log.info("清理知识文档旧切片完成，documentId={}, removed={}", documentId, removed);
    }

    /**
     * 校验知识文档实体。
     *
     * @param document 知识文档实体
     */
    private void validateDocument(KnowledgeDocument document) {
        BizAssert.notNull(document, ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.DOCUMENT_REQUIRED);
        BizAssert.notNull(document.getId(), ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.DOCUMENT_ID_REQUIRED);
    }
}