package com.xcvk.platform.knowledge.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.common.util.DbAssert;
import com.xcvk.platform.knowledge.assembler.KnowledgeDocumentAssembler;
import com.xcvk.platform.knowledge.constant.KnowledgeErrorMessages;
import com.xcvk.platform.knowledge.model.cmd.CreateKnowledgeDocumentCmd;
import com.xcvk.platform.knowledge.model.dto.CreateKnowledgeDocumentRequest;
import com.xcvk.platform.knowledge.model.dto.UpdateKnowledgeDocumentRequest;
import com.xcvk.platform.knowledge.model.entity.KnowledgeDocument;
import com.xcvk.platform.knowledge.repository.mapper.KnowledgeDocumentMapper;
import com.xcvk.platform.knowledge.search.assembler.KnowledgeSearchAssembler;
import com.xcvk.platform.knowledge.search.repository.KnowledgeDocumentIndexRepository;
import com.xcvk.platform.knowledge.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 知识文档服务实现类
 *
 * <p>当前阶段优先完成知识文档主链闭环：</p>
 * <ul>
 *     <li>创建知识文档</li>
 *     <li>写入 MySQL 主库</li>
 *     <li>同步写入 Elasticsearch 搜索索引</li>
 * </ul>
 *
 * <p>Elasticsearch 当前作为搜索副本，不作为业务真相来源。
 * 因此 ES 同步失败时不直接回滚主业务，后续可扩展 MQ 或补偿任务。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-24
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeDocumentServiceImpl
        extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument>
        implements KnowledgeDocumentService {

    private final KnowledgeDocumentAssembler knowledgeDocumentAssembler;

    private final KnowledgeSearchAssembler knowledgeSearchAssembler;

    private final KnowledgeDocumentIndexRepository knowledgeDocumentIndexRepository;

    /**
     * 创建知识文档。
     *
     * <p>当前阶段创建后默认发布，并同步写入 Elasticsearch，
     * 用于先跑通知识库全文检索的最小闭环。</p>
     *
     * @param identity 当前登录身份
     * @param request 创建知识文档请求
     * @return 知识文档ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDocument(CurrentLoginIdentity identity, CreateKnowledgeDocumentRequest request) {
        validateCurrentLoginIdentity(identity);
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.CREATE_DOCUMENT_REQUEST_REQUIRED);

        CreateKnowledgeDocumentCmd cmd = knowledgeDocumentAssembler.toCreateCmd(identity, request);
        validateCreateCmd(cmd);

        KnowledgeDocument document = knowledgeDocumentAssembler.toEntity(cmd);

        int rows = baseMapper.insert(document);
        DbAssert.affectedOne(rows, KnowledgeErrorMessages.CREATE_DOCUMENT_FAILED);

        syncDocumentToSearchIndex(document);

        return document.getId();
    }

    /**
     * 更新知识文档。
     *
     * <p>当前阶段更新后默认发布，并同步写入 Elasticsearch，
     * 用于先跑通知识库全文检索的最小闭环。</p>
     *
     * @param identity 当前登录身份
     * @param documentId 知识文档ID
     * @param request 更新知识文档请求
     */
    // TODO 后续修改逻辑为es查询失败降级MySQL
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDocument(CurrentLoginIdentity identity,
                               Long documentId,
                               UpdateKnowledgeDocumentRequest request) {
        validateCurrentLoginIdentity(identity);
        validateUpdateRequest(documentId, request);

        KnowledgeDocument existDocument = getById(documentId);
        BizAssert.notNull(existDocument, ErrorCode.BIZ_ERROR, KnowledgeErrorMessages.DOCUMENT_NOT_FOUND);

        KnowledgeDocument updateEntity = knowledgeDocumentAssembler.toUpdateEntity(documentId, request);

        int rows = baseMapper.updateById(updateEntity);
        DbAssert.affectedOne(rows, KnowledgeErrorMessages.UPDATE_DOCUMENT_FAILED);

        KnowledgeDocument latestDocument = getById(documentId);
        syncDocumentToSearchIndex(latestDocument);
    }

    /**
     * 校验更新知识文档请求。
     *
     * @param documentId 知识文档ID
     * @param request 更新知识文档请求
     */
    private void validateUpdateRequest(Long documentId, UpdateKnowledgeDocumentRequest request) {
        BizAssert.notNull(documentId, ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.DOCUMENT_ID_REQUIRED);
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.UPDATE_DOCUMENT_REQUEST_REQUIRED);
        BizAssert.hasText(request.title(), ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.TITLE_REQUIRED);
        BizAssert.hasText(request.content(), ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.CONTENT_REQUIRED);
    }

    /**
     * 校验创建知识文档命令对象的基础必填字段。
     *
     * @param cmd 创建知识文档命令对象
     */
    private void validateCreateCmd(CreateKnowledgeDocumentCmd cmd) {
        BizAssert.notNull(cmd, ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.CREATE_DOCUMENT_CMD_REQUIRED);

        BizAssert.hasText(cmd.title(), ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.TITLE_REQUIRED);
        BizAssert.hasText(cmd.content(), ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.CONTENT_REQUIRED);
        BizAssert.notNull(cmd.creatorId(), ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.CREATOR_ID_REQUIRED);
        BizAssert.hasText(cmd.creatorName(), ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.CREATOR_NAME_REQUIRED);
    }

    /**
     * 校验当前登录身份。
     *
     * @param identity 当前登录身份
     */
    private void validateCurrentLoginIdentity(CurrentLoginIdentity identity) {
        BizAssert.notNull(identity, ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.CURRENT_LOGIN_IDENTITY_REQUIRED);
        BizAssert.notNull(identity.userId(), ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.CURRENT_USER_ID_REQUIRED);
        BizAssert.hasText(identity.username(), ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.CURRENT_USERNAME_REQUIRED);
    }

    /**
     * 同步知识文档到搜索索引。
     *
     * <p>当前阶段 Elasticsearch 作为搜索副本，不属于主业务真相来源，
     * 因此同步失败不直接中断主业务流程，而是记录错误日志，
     * 便于后续通过补偿任务或 MQ 机制修复。</p>
     *
     * @param document 知识文档实体
     */
    private void syncDocumentToSearchIndex(KnowledgeDocument document) {
        if (document == null) {
            return;
        }

        try {
            knowledgeDocumentIndexRepository.save(knowledgeSearchAssembler.toIndex(document));
            log.info("同步知识文档到搜索索引成功：{}", document.getId());
        } catch (Exception ex) {
            log.error("同步知识文档到搜索索引失败，documentId={}", document.getId(), ex);
        }
    }
}