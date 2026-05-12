package com.xcvk.platform.ai.service.rag;

import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextItem;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * RAG 召回相关性判断器。
 */
@Component
public class RagRelevanceEvaluator {

    private static final int RELEVANCE_TOP_WINDOW = 3;

    private static final String MATCH_TYPE_TEXT = "TEXT";

    private static final String MATCH_TYPE_BOTH = "BOTH";

    /**
     * 判断召回结果是否低相关。
     *
     * <p>不使用固定 finalScore 阈值，因为 finalScore 在不同阶段可能是不同语义的融合分。</p>
     */
    public boolean isLowRelevance(List<KnowledgeRagContextItem> contexts) {
        if (CollectionUtils.isEmpty(contexts)) {
            return true;
        }

        int topWindow = Math.min(RELEVANCE_TOP_WINDOW, contexts.size());

        boolean hasHybridEvidenceInTopWindow = contexts.stream()
                .limit(topWindow)
                .anyMatch(context -> MATCH_TYPE_BOTH.equalsIgnoreCase(context.matchType()));

        if (hasHybridEvidenceInTopWindow) {
            return false;
        }

        boolean hasTextEvidenceInTopWindow = contexts.stream()
                .limit(topWindow)
                .anyMatch(context -> MATCH_TYPE_TEXT.equalsIgnoreCase(context.matchType()));

        return !hasTextEvidenceInTopWindow;
    }
}