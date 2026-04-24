package com.xcvk.platform.knowledge.assembler;

import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.common.enums.DeleteStatus;
import com.xcvk.platform.knowledge.constant.KnowledgeDocumentStatusConstants;
import com.xcvk.platform.knowledge.model.cmd.CreateKnowledgeDocumentCmd;
import com.xcvk.platform.knowledge.model.dto.CreateKnowledgeDocumentRequest;
import com.xcvk.platform.knowledge.model.entity.KnowledgeDocument;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 知识文档转换器
 *
 * <p>用于收口知识文档相关对象转换逻辑，避免 Controller 和 Service 中散落对象组装代码。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-24
 */
@Component
public class KnowledgeDocumentAssembler {

    public CreateKnowledgeDocumentCmd toCreateCmd(CurrentLoginIdentity identity,
                                                  CreateKnowledgeDocumentRequest request) {
        return new CreateKnowledgeDocumentCmd(
                request.title(),
                request.summary(),
                request.content(),
                request.categoryId(),
                request.categoryName(),
                request.tags(),
                identity.userId(),
                identity.username()
        );
    }

    public KnowledgeDocument toEntity(CreateKnowledgeDocumentCmd cmd) {
        LocalDateTime now = LocalDateTime.now();

        return new KnowledgeDocument()
                .setTitle(cmd.title())
                .setSummary(cmd.summary())
                .setContent(cmd.content())
                .setCategoryId(cmd.categoryId())
                .setCategoryName(cmd.categoryName())
                .setTags(cmd.tags())
                .setStatus(KnowledgeDocumentStatusConstants.PUBLISHED)
                .setCreatorId(cmd.creatorId())
                .setCreatorName(cmd.creatorName())
                .setPublishedAt(now)
                .setCreatedAt(now)
                .setUpdatedAt(now)
                .setDeleted(DeleteStatus.NORMAL);
    }
}