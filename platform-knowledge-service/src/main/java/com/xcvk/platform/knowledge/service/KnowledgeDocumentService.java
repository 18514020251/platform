package com.xcvk.platform.knowledge.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.knowledge.model.dto.CreateKnowledgeDocumentRequest;
import com.xcvk.platform.knowledge.model.dto.UpdateKnowledgeDocumentRequest;
import com.xcvk.platform.knowledge.model.entity.KnowledgeDocument;
import com.xcvk.platform.knowledge.model.vo.KnowledgeDocumentDetailVO;

/**
 * 知识文档服务接口
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-24
 */
public interface KnowledgeDocumentService extends IService<KnowledgeDocument> {

    /**
     * 创建知识文档
     *
     * @param identity 当前登录用户信息
     * @param request 创建知识文档请求
     * @return 知识文档ID
     */
    Long createDocument(CurrentLoginIdentity identity, CreateKnowledgeDocumentRequest request);

    /**
     * 查询知识文档详情。
     *
     * @param documentId 知识文档ID
     * @return 知识文档详情
     */
    KnowledgeDocumentDetailVO getDocumentDetail(Long documentId);

    /**
     * 更新知识文档
     *
     * @param identity 当前登录用户信息
     * @param documentId 知识文档ID
     * @param request 更新知识文档请求
     */
    void updateDocument(CurrentLoginIdentity identity,
                        Long documentId,
                        UpdateKnowledgeDocumentRequest request);

    /**
     * 上线知识文档。
     *
     * @param identity 当前登录身份
     * @param documentId 文档ID
     */
    void onlineDocument(CurrentLoginIdentity identity, Long documentId);

    /**
     * 下线知识文档。
     *
     * @param identity 当前登录身份
     * @param documentId 文档ID
     */
    void offlineDocument(CurrentLoginIdentity identity, Long documentId);

}