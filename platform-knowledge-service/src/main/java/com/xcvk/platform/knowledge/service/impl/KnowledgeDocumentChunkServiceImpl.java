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
import com.xcvk.platform.knowledge.search.assembler.KnowledgeChunkSearchAssembler;
import com.xcvk.platform.knowledge.search.model.index.KnowledgeChunkIndex;
import com.xcvk.platform.knowledge.search.repository.KnowledgeChunkIndexRepository;
import com.xcvk.platform.knowledge.service.KnowledgeDocumentChunkService;
import com.xcvk.platform.knowledge.support.KnowledgeChunkSplitter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 知识文档切片服务实现类
 *
 * <p>当前阶段负责维护 MySQL 中的知识文档切片数据，
 * 并同步维护 Elasticsearch 中的 chunk 级搜索索引。</p>
 *
 * <p>MySQL chunk 是可追踪、可重建的中间数据；
 * ES chunk 是检索副本，后续会继续扩展 embedding 和向量检索能力。</p>
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

    private final KnowledgeChunkSearchAssembler knowledgeChunkSearchAssembler;

    private final KnowledgeChunkIndexRepository knowledgeChunkIndexRepository;

    /**
     * 重建知识文档切片。
     *
     * <p>当前阶段采用简单策略：先删除旧切片，再根据最新正文重新生成新切片。
     * 这种方式实现简单、结果确定，适合第一版最小闭环。</p>
     *
     * <p>同时会同步维护 ES chunk 索引：
     * 删除旧 chunk 索引，再写入新 chunk 索引。</p>
     *
     * @param document 知识文档实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildDocumentChunks(KnowledgeDocument document) {
        validateDocument(document);

        List<KnowledgeDocumentChunk> oldChunks = listChunksByDocumentId(document.getId());

        removeOldChunks(document.getId());

        List<String> chunkTexts = knowledgeChunkSplitter.split(document.getContent());
        List<KnowledgeDocumentChunk> chunks = knowledgeDocumentChunkAssembler.toChunks(document.getId(), chunkTexts);

        if (CollectionUtils.isEmpty(chunks)) {
            deleteOldChunkIndexes(oldChunks);
            log.warn("知识文档未生成有效切片，documentId={}", document.getId());
            return;
        }

        for (KnowledgeDocumentChunk chunk : chunks) {
            int rows = baseMapper.insert(chunk);
            DbAssert.affectedOne(rows, KnowledgeErrorMessages.SAVE_DOCUMENT_CHUNK_FAILED);
        }

        deleteOldChunkIndexes(oldChunks);
        syncChunksToSearchIndex(document, chunks);

        log.info("重建知识文档切片成功，documentId={}, chunkCount={}", document.getId(), chunks.size());
    }

    /**
     * 下线知识文档切片。
     *
     * <p>下线文档时，chunk 作为派生检索数据也需要同步下线，
     * 避免后续 chunk 级全文检索、向量检索或混合检索召回已下线内容。</p>
     *
     * @param document 知识文档实体
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offlineDocumentChunks(KnowledgeDocument document) {
        validateDocument(document);

        KnowledgeDocumentChunk updateEntity = new KnowledgeDocumentChunk()
                .setStatus(KnowledgeChunkStatusConstants.OFFLINE)
                .setUpdatedAt(LocalDateTime.now());

        LambdaUpdateWrapper<KnowledgeDocumentChunk> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(KnowledgeDocumentChunk::getDocumentId, document.getId())
                .eq(KnowledgeDocumentChunk::getStatus, KnowledgeChunkStatusConstants.ACTIVE);

        int rows = baseMapper.update(updateEntity, updateWrapper);

        List<KnowledgeDocumentChunk> latestChunks = listChunksByDocumentId(document.getId());
        syncChunksToSearchIndex(document, latestChunks);

        log.info("下线知识文档切片完成，documentId={}, affectedRows={}", document.getId(), rows);
    }

    /**
     * 查询指定文档的全部切片。
     *
     * @param documentId 知识文档ID
     * @return 知识文档切片列表
     */
    private List<KnowledgeDocumentChunk> listChunksByDocumentId(Long documentId) {
        return this.list(new LambdaQueryWrapper<KnowledgeDocumentChunk>()
                .eq(KnowledgeDocumentChunk::getDocumentId, documentId)
                .orderByAsc(KnowledgeDocumentChunk::getChunkNo));
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
     * 删除旧 chunk 搜索索引。
     *
     * <p>ES 当前作为检索副本，因此删除失败不直接中断主业务流程，
     * 只记录日志，后续可以通过补偿任务修复。</p>
     *
     * @param oldChunks 旧切片列表
     */
    private void deleteOldChunkIndexes(List<KnowledgeDocumentChunk> oldChunks) {
        if (CollectionUtils.isEmpty(oldChunks)) {
            return;
        }

        List<Long> oldChunkIds = oldChunks.stream()
                .map(KnowledgeDocumentChunk::getId)
                .toList();

        try {
            knowledgeChunkIndexRepository.deleteAllById(oldChunkIds);
            log.info("删除旧知识切片搜索索引成功，chunkCount={}", oldChunkIds.size());
        } catch (Exception ex) {
            log.error("删除旧知识切片搜索索引失败，chunkCount={}", oldChunkIds.size(), ex);
        }
    }

    /**
     * 同步 chunk 到搜索索引。
     *
     * <p>当前阶段同步 chunk 级全文检索字段，
     * 后续接入 embedding 后，可在 KnowledgeChunkIndex 中继续补充 dense_vector 字段。</p>
     *
     * @param document 知识文档实体
     * @param chunks 知识文档切片列表
     */
    private void syncChunksToSearchIndex(KnowledgeDocument document, List<KnowledgeDocumentChunk> chunks) {
        if (document == null || CollectionUtils.isEmpty(chunks)) {
            return;
        }

        List<KnowledgeChunkIndex> indexes = chunks.stream()
                .map(chunk -> knowledgeChunkSearchAssembler.toIndex(document, chunk))
                .filter(Objects::nonNull)
                .toList();

        if (CollectionUtils.isEmpty(indexes)) {
            return;
        }

        try {
            knowledgeChunkIndexRepository.saveAll(indexes);
            log.info("同步知识文档切片到搜索索引成功，documentId={}, chunkCount={}",
                    document.getId(), indexes.size());
        } catch (Exception ex) {
            log.error("同步知识文档切片到搜索索引失败，documentId={}, chunkCount={}",
                    document.getId(), indexes.size(), ex);
        }
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