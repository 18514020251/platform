package com.xcvk.platform.ai.service.impl;

import com.xcvk.platform.ai.model.dto.EmbeddingRequest;
import com.xcvk.platform.ai.model.vo.EmbeddingResponse;
import com.xcvk.platform.ai.properties.DashScopeEmbeddingProperties;
import com.xcvk.platform.ai.service.EmbeddingService;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 文本向量化服务实现类
 *
 * <p>当前阶段基于 LangChain4j EmbeddingModel 调用 DashScope text-embedding-v3，
 * 将文本列表转换为向量列表。</p>
 *
 * <p>该服务只负责模型调用，不负责知识文档、chunk、ES 索引等业务逻辑。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-27
 */
@Service
@RequiredArgsConstructor
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingModel embeddingModel;

    private final DashScopeEmbeddingProperties dashScopeEmbeddingProperties;

    /**
     * 批量生成文本向量。
     *
     * @param request 文本向量化请求
     * @return 文本向量化响应
     */
    @Override
    public EmbeddingResponse embedTexts(EmbeddingRequest request) {
        validateRequest(request);

        List<TextSegment> segments = request.texts().stream()
                .filter(StringUtils::hasText)
                .map(text -> TextSegment.from(text.trim()))
                .toList();

        BizAssert.isTrue(!CollectionUtils.isEmpty(segments), ErrorCode.PARAM_INVALID, "有效向量化文本不能为空");

        Response<List<Embedding>> response = embeddingModel.embedAll(segments);

        List<List<Float>> vectors = response.content().stream()
                .map(Embedding::vectorAsList)
                .toList();

        return new EmbeddingResponse(
                dashScopeEmbeddingProperties.getModelName(),
                embeddingModel.dimension(),
                vectors
        );
    }

    /**
     * 校验文本向量化请求。
     *
     * @param request 文本向量化请求
     */
    private void validateRequest(EmbeddingRequest request) {
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, "文本向量化请求不能为空");
        BizAssert.isTrue(!CollectionUtils.isEmpty(request.texts()), ErrorCode.PARAM_INVALID, "向量化文本不能为空");
    }
}