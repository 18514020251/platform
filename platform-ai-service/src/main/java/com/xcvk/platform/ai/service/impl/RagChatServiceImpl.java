package com.xcvk.platform.ai.service.impl;

import com.xcvk.platform.ai.model.dto.RagChatRequest;
import com.xcvk.platform.ai.model.vo.RagChatResponse;
import com.xcvk.platform.ai.service.RagChatService;
import com.xcvk.platform.api.contract.knowledge.client.KnowledgeRagContextClient;
import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextItem;
import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextRequest;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * RAG 问答服务实现类
 *
 * <p>当前阶段实现最小 RAG 闭环：</p>
 * <ul>
 *     <li>调用 knowledge-service 召回知识上下文</li>
 *     <li>将上下文和用户问题组装为 Prompt</li>
 *     <li>调用大模型生成回答</li>
 * </ul>
 *
 * <p>当前版本不做流式输出、多轮记忆、引用格式增强等复杂能力。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-29
 */
@Service
@RequiredArgsConstructor
public class RagChatServiceImpl implements RagChatService {

    private static final String NO_CONTEXT_ANSWER = "知识库中暂未检索到相关内容，无法基于现有知识库回答该问题。";

    private final KnowledgeRagContextClient knowledgeRagContextClient;

    private final ChatModel chatModel;

    /**
     * 基于知识库进行问答。
     *
     * @param request RAG 问答请求
     * @return RAG 问答响应
     */
    @Override
    public RagChatResponse chat(RagChatRequest request) {
        validateRequest(request);

        List<KnowledgeRagContextItem> contexts = retrieveContexts(request);

        if (CollectionUtils.isEmpty(contexts)) {
            return new RagChatResponse(NO_CONTEXT_ANSWER, List.of());
        }

        String prompt = buildPrompt(request.question(), contexts);
        String answer = chatModel.chat(prompt);

        return new RagChatResponse(answer, contexts);
    }

    /**
     * 召回知识库上下文。
     *
     * @param request RAG 问答请求
     * @return 知识上下文列表
     */
    private List<KnowledgeRagContextItem> retrieveContexts(RagChatRequest request) {
        KnowledgeRagContextRequest contextRequest = new KnowledgeRagContextRequest(
                request.question(),
                request.safeTopK(),
                request.categoryId()
        );

        List<KnowledgeRagContextItem> contexts = knowledgeRagContextClient.retrieveContexts(contextRequest);
        return contexts == null ? List.of() : contexts;
    }

    /**
     * 构建 RAG Prompt。
     *
     * @param question 用户问题
     * @param contexts 知识上下文
     * @return Prompt 文本
     */
    private String buildPrompt(String question, List<KnowledgeRagContextItem> contexts) {
        StringBuilder builder = new StringBuilder();

        builder.append("你是企业知识库助手，请严格基于【知识库内容】回答用户问题。\n");
        builder.append("如果知识库内容不足以回答，请明确说明无法从当前知识库中确定，不要编造。\n\n");

        builder.append("【知识库内容】\n");
        for (int i = 0; i < contexts.size(); i++) {
            KnowledgeRagContextItem context = contexts.get(i);

            builder.append("片段").append(i + 1).append("：\n");
            builder.append("文档标题：").append(safeText(context.documentTitle())).append("\n");
            builder.append("分类：").append(safeText(context.categoryName())).append("\n");
            builder.append("内容：\n").append(safeText(context.content())).append("\n\n");
        }

        builder.append("【用户问题】\n");
        builder.append(question.trim()).append("\n\n");

        builder.append("【回答要求】\n");
        builder.append("1. 只基于知识库内容回答。\n");
        builder.append("2. 回答要简洁、清晰。\n");
        builder.append("3. 如果涉及对比，请用条理化方式说明。\n");

        return builder.toString();
    }

    /**
     * 校验 RAG 问答请求。
     *
     * @param request RAG 问答请求
     */
    private void validateRequest(RagChatRequest request) {
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, "RAG问答请求不能为空");
        BizAssert.hasText(request.question(), ErrorCode.PARAM_INVALID, "问题不能为空");
    }

    /**
     * 安全文本。
     *
     * @param value 原始文本
     * @return 非 null 文本
     */
    private String safeText(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }
}