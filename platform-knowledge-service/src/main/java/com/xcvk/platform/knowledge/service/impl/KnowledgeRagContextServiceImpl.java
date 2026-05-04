package com.xcvk.platform.knowledge.service.impl;

import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.knowledge.constant.KnowledgeErrorMessages;
import com.xcvk.platform.knowledge.model.dto.KnowledgeChunkHybridSearchRequest;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkHybridSearchItemVO;
import com.xcvk.platform.knowledge.model.vo.KnowledgeRagContextItemVO;
import com.xcvk.platform.knowledge.search.service.KnowledgeChunkHybridSearchService;
import com.xcvk.platform.knowledge.service.KnowledgeRagContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * RAG 知识上下文召回服务实现类
 *
 * <p>当前阶段复用 chunk 混合检索能力，
 * 将检索结果转换为适合大模型 Prompt 使用的干净知识片段。</p>
 *
 * <p>注意：混合检索返回的 chunkText 可能包含 em 高亮标签，
 * 本服务会移除这些展示标签，避免污染大模型输入。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
@Service
@RequiredArgsConstructor
public class KnowledgeRagContextServiceImpl implements KnowledgeRagContextService {

    private static final String HIGHLIGHT_PRE_TAG = "<em>";

    private static final String HIGHLIGHT_POST_TAG = "</em>";

    private final KnowledgeChunkHybridSearchService knowledgeChunkHybridSearchService;

    /**
     * 召回 RAG 知识上下文。
     *
     * @param request 混合检索请求
     * @return 干净的知识上下文片段列表
     */
    @Override
    public List<KnowledgeRagContextItemVO> retrieveContexts(KnowledgeChunkHybridSearchRequest request) {
        validateRequest(request);

        List<KnowledgeChunkHybridSearchItemVO> hybridResults =
                knowledgeChunkHybridSearchService.hybridSearch(request);

        return hybridResults.stream()
                .map(this::toRagContextItemVO)
                .toList();
    }

    /**
     * 将混合检索结果转换为 RAG 上下文片段。
     *
     * @param item 混合检索结果项
     * @return RAG 上下文片段
     */
    private KnowledgeRagContextItemVO toRagContextItemVO(KnowledgeChunkHybridSearchItemVO item) {
        return new KnowledgeRagContextItemVO(
                item.chunkId(),
                item.documentId(),
                item.chunkNo(),
                item.documentTitle(),
                item.categoryName(),
                cleanHighlightTags(item.chunkText()),
                item.finalScore(),
                item.matchType()
        );
    }

    /**
     * 清理高亮标签。
     *
     * <p>当前只清理检索高亮使用的 em 标签，不做复杂 HTML 处理。</p>
     *
     * @param text 原始文本
     * @return 清理后的文本
     */
    private String cleanHighlightTags(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }

        return text.replace(HIGHLIGHT_PRE_TAG, "")
                .replace(HIGHLIGHT_POST_TAG, "");
    }

    /**
     * 校验请求参数。
     *
     * @param request 混合检索请求
     */
    private void validateRequest(KnowledgeChunkHybridSearchRequest request) {
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.HYBRID_SEARCH_REQUEST_REQUIRED);
        BizAssert.hasText(request.question(), ErrorCode.PARAM_INVALID, KnowledgeErrorMessages.HYBRID_SEARCH_QUESTION_REQUIRED);
    }
}