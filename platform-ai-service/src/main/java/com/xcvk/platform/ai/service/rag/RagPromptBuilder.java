package com.xcvk.platform.ai.service.rag;

import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextItem;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * RAG Prompt 构建器。
 */
@Component
public class RagPromptBuilder {

    private static final String PROMPT_SYSTEM_ROLE = """
            你是企业知识库助手，请严格基于【知识库内容】回答用户问题。
            如果知识库内容不足以回答，请明确说明无法从当前知识库中确定，不要编造。
            """;

    private static final String PROMPT_KNOWLEDGE_BASE_TITLE = "【知识库内容】\n";

    private static final String PROMPT_SECTION_TEMPLATE = """
            片段{index}：
            文档标题：{documentTitle}
            分类：{categoryName}
            内容：
            {content}
            
            """;

    private static final String PROMPT_QUESTION_TITLE = "【用户问题】\n";

    private static final String PROMPT_REWRITTEN_QUESTION_TITLE = "【检索增强问题】\n";

    private static final String PROMPT_REQUIREMENTS_TITLE = "【回答要求】\n";

    private static final String PROMPT_REQUIREMENTS_CONTENT = """
            1. 只基于知识库内容回答，不要编造。
            2. 如果知识库片段中已经出现相关章节、条款或明确描述，不要声称知识库未明确说明。
            3. 回答要简洁、清晰，优先使用条目化列表。
            4. 回答末尾请标注依据的片段编号，例如：依据：片段2、片段3。
            5. 如果知识库内容确实不足，请只说明“当前知识库未提供足够信息”，不要扩展推测。
            6. 如果多个片段都与问题相关，请综合所有相关片段，不要只回答其中一个片段中的部分条目。
            """;

    private static final String PROMPT_REWRITE_REQUIREMENTS_CONTENT = """
            7. 检索增强问题只能用于辅助理解知识库片段，不能作为新的用户问题，也不能基于它引入额外假设。
            8. 如果用户原始问题和检索增强问题存在差异，请优先遵循用户原始问题。
            """;

    public String buildEnhancedAnswerPrompt(String originalQuestion,
                                            String rewrittenQuestion,
                                            List<KnowledgeRagContextItem> contexts) {
        return buildAnswerPrompt(originalQuestion, rewrittenQuestion, contexts, true);
    }

    public String buildAnswerPrompt(String question,
                                    List<KnowledgeRagContextItem> contexts) {
        return buildAnswerPrompt(question, null, contexts, false);
    }

    private String buildAnswerPrompt(String question,
                                     String rewrittenQuestion,
                                     List<KnowledgeRagContextItem> contexts,
                                     boolean includeRewriteInfo) {
        StringBuilder builder = new StringBuilder();

        builder.append(PROMPT_SYSTEM_ROLE).append("\n");

        builder.append(PROMPT_KNOWLEDGE_BASE_TITLE);
        appendKnowledgeContexts(builder, contexts);

        builder.append(PROMPT_QUESTION_TITLE);
        builder.append(safeText(question)).append("\n\n");

        if (includeRewriteInfo
                && StringUtils.hasText(rewrittenQuestion)
                && !safeText(question).equals(safeText(rewrittenQuestion))) {
            builder.append(PROMPT_REWRITTEN_QUESTION_TITLE);
            builder.append(safeText(rewrittenQuestion)).append("\n");
            builder.append("说明：检索增强问题是系统为了召回知识库内容生成的辅助表达，")
                    .append("最终回答必须以用户原始问题为准，不要偏离用户原始意图。")
                    .append("\n\n");
        }

        builder.append(PROMPT_REQUIREMENTS_TITLE);
        builder.append(PROMPT_REQUIREMENTS_CONTENT);

        if (includeRewriteInfo
                && StringUtils.hasText(rewrittenQuestion)
                && !safeText(question).equals(safeText(rewrittenQuestion))) {
            builder.append(PROMPT_REWRITE_REQUIREMENTS_CONTENT);
        }

        return builder.toString();
    }

    private void appendKnowledgeContexts(StringBuilder builder, List<KnowledgeRagContextItem> contexts) {
        if (CollectionUtils.isEmpty(contexts)) {
            return;
        }

        for (int index = 0; index < contexts.size(); index++) {
            KnowledgeRagContextItem context = contexts.get(index);
            builder.append(PROMPT_SECTION_TEMPLATE
                    .replace("{index}", String.valueOf(index + 1))
                    .replace("{documentTitle}", safeText(context.documentTitle()))
                    .replace("{categoryName}", safeText(context.categoryName()))
                    .replace("{content}", safeText(context.content())));
        }
    }

    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}