package com.xcvk.platform.knowledge.search.service.impl;

import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.common.exception.BusinessException;
import com.xcvk.platform.knowledge.constant.KnowledgeChunkStatusConstants;
import com.xcvk.platform.knowledge.model.dto.KnowledgeChunkHybridSearchRequest;
import com.xcvk.platform.knowledge.model.dto.KnowledgeChunkVectorSearchRequest;
import com.xcvk.platform.knowledge.model.query.KnowledgeChunkSearchQuery;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkHybridSearchItemVO;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkSearchItemVO;
import com.xcvk.platform.knowledge.model.vo.KnowledgeChunkVectorSearchItemVO;
import com.xcvk.platform.knowledge.search.service.KnowledgeChunkSearchService;
import com.xcvk.platform.knowledge.search.service.KnowledgeChunkVectorSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class KnowledgeChunkHybridSearchServiceTest {

    private KnowledgeChunkSearchService textSearchService;
    private KnowledgeChunkVectorSearchService vectorSearchService;
    private KnowledgeChunkHybridSearchServiceImpl hybridSearchService;

    @BeforeEach
    void setUp() {
        textSearchService = mock(KnowledgeChunkSearchService.class);
        vectorSearchService = mock(KnowledgeChunkVectorSearchService.class);

        hybridSearchService = new KnowledgeChunkHybridSearchServiceImpl(
                textSearchService,
                vectorSearchService
        );
    }

    @Test
    @DisplayName("请求为空时应该抛出业务异常")
    void shouldRejectWhenRequestIsNull() {
        assertThrows(
                BusinessException.class,
                () -> hybridSearchService.hybridSearch(null)
        );

        verifyNoInteractions(textSearchService);
        verifyNoInteractions(vectorSearchService);
    }

    @Test
    @DisplayName("问题为空白字符串时应该抛出业务异常")
    void shouldRejectWhenQuestionIsBlank() {
        KnowledgeChunkHybridSearchRequest request = new KnowledgeChunkHybridSearchRequest(
                "   ",
                5,
                null
        );

        assertThrows(
                BusinessException.class,
                () -> hybridSearchService.hybridSearch(request)
        );

        verifyNoInteractions(textSearchService);
        verifyNoInteractions(vectorSearchService);
    }

    @Test
    @DisplayName("混合检索应该同时调用全文检索和向量检索，并传入正确的召回数量")
    void shouldCallTextAndVectorSearchWithResolvedRecallSize() {
        KnowledgeChunkHybridSearchRequest request = new KnowledgeChunkHybridSearchRequest(
                "如何重置密码",
                5,
                10L
        );

        when(textSearchService.searchChunks(any()))
                .thenReturn(PageResult.of(List.of(), 0, 1, 10));
        when(vectorSearchService.vectorSearch(any()))
                .thenReturn(List.of());

        hybridSearchService.hybridSearch(request);

        ArgumentCaptor<KnowledgeChunkSearchQuery> textQueryCaptor =
                ArgumentCaptor.forClass(KnowledgeChunkSearchQuery.class);

        ArgumentCaptor<KnowledgeChunkVectorSearchRequest> vectorRequestCaptor =
                ArgumentCaptor.forClass(KnowledgeChunkVectorSearchRequest.class);

        verify(textSearchService).searchChunks(textQueryCaptor.capture());
        verify(vectorSearchService).vectorSearch(vectorRequestCaptor.capture());

        KnowledgeChunkSearchQuery textQuery = textQueryCaptor.getValue();
        KnowledgeChunkVectorSearchRequest vectorRequest = vectorRequestCaptor.getValue();

        assertEquals("如何重置密码", textQuery.keyword());
        assertEquals(10L, textQuery.categoryId());
        assertEquals(KnowledgeChunkStatusConstants.ACTIVE, textQuery.status());
        assertEquals(1, textQuery.pageNum());

        /*
         * topK = 5
         * 内部召回倍数 = 2
         * 所以全文检索和向量检索各召回 10 条候选。
         */
        assertEquals(10, textQuery.pageSize());
        assertEquals("如何重置密码", vectorRequest.question());
        assertEquals(10, vectorRequest.topK());
        assertEquals(10L, vectorRequest.categoryId());
    }

    @Test
    @DisplayName("内部召回数量最大不能超过 20")
    void shouldCapRecallSizeToTwenty() {
        KnowledgeChunkHybridSearchRequest request = new KnowledgeChunkHybridSearchRequest(
                "系统无法登录怎么办",
                20,
                null
        );

        when(textSearchService.searchChunks(any()))
                .thenReturn(PageResult.of(List.of(), 0, 1, 20));
        when(vectorSearchService.vectorSearch(any()))
                .thenReturn(List.of());

        hybridSearchService.hybridSearch(request);

        ArgumentCaptor<KnowledgeChunkSearchQuery> textQueryCaptor =
                ArgumentCaptor.forClass(KnowledgeChunkSearchQuery.class);

        ArgumentCaptor<KnowledgeChunkVectorSearchRequest> vectorRequestCaptor =
                ArgumentCaptor.forClass(KnowledgeChunkVectorSearchRequest.class);

        verify(textSearchService).searchChunks(textQueryCaptor.capture());
        verify(vectorSearchService).vectorSearch(vectorRequestCaptor.capture());

        /*
         * topK = 20，按 topK * 2 本来是 40。
         * 但服务里限制 MAX_RECALL_SIZE = 20，所以实际只召回 20。
         */
        assertEquals(20, textQueryCaptor.getValue().pageSize());
        assertEquals(20, vectorRequestCaptor.getValue().topK());
    }

    @Test
    @DisplayName("同一个 chunk 同时被全文和向量命中时，应该合并为 BOTH")
    void shouldMergeSameChunkAsBothMatch() {
        KnowledgeChunkHybridSearchRequest request = new KnowledgeChunkHybridSearchRequest(
                "账号被冻结怎么办",
                5,
                null
        );

        KnowledgeChunkSearchItemVO textItem = textItem(1001L, "账号被冻结后需要联系管理员");
        KnowledgeChunkVectorSearchItemVO vectorItem = vectorItem(1001L, "账号被冻结后需要联系管理员", 0.93F);

        when(textSearchService.searchChunks(any()))
                .thenReturn(PageResult.of(List.of(textItem), 1, 1, 10));
        when(vectorSearchService.vectorSearch(any()))
                .thenReturn(List.of(vectorItem));

        List<KnowledgeChunkHybridSearchItemVO> results = hybridSearchService.hybridSearch(request);

        assertEquals(1, results.size());

        KnowledgeChunkHybridSearchItemVO result = results.get(0);

        assertEquals(1001L, result.chunkId());
        assertEquals("BOTH", result.matchType());

        assertNotNull(result.textRankScore());
        assertNotNull(result.vectorRankScore());
        assertEquals(0.93F, result.vectorRawScore());

        /*
         * 同时命中文本和向量时，finalScore 应该大于任一单路 RRF 分数。
         * 这里不强行写死具体小数，避免浮点数比较过脆。
         */
        assertTrue(result.finalScore() > result.textRankScore());
        assertTrue(result.finalScore() > result.vectorRankScore());
    }

    @Test
    @DisplayName("只被全文命中的 chunk 应该标记为 TEXT")
    void shouldMarkTextOnlyMatchAsText() {
        KnowledgeChunkHybridSearchRequest request = new KnowledgeChunkHybridSearchRequest(
                "如何修改手机号",
                5,
                null
        );

        KnowledgeChunkSearchItemVO textItem = textItem(2001L, "用户可以在个人中心修改手机号");

        when(textSearchService.searchChunks(any()))
                .thenReturn(PageResult.of(List.of(textItem), 1, 1, 10));
        when(vectorSearchService.vectorSearch(any()))
                .thenReturn(List.of());

        List<KnowledgeChunkHybridSearchItemVO> results = hybridSearchService.hybridSearch(request);

        assertEquals(1, results.size());

        KnowledgeChunkHybridSearchItemVO result = results.get(0);

        assertEquals(2001L, result.chunkId());
        assertEquals("TEXT", result.matchType());
        assertNotNull(result.textRankScore());
        assertEquals(null, result.vectorRankScore());
        assertEquals(null, result.vectorRawScore());
    }

    @Test
    @DisplayName("只被向量命中的 chunk 应该标记为 VECTOR")
    void shouldMarkVectorOnlyMatchAsVector() {
        KnowledgeChunkHybridSearchRequest request = new KnowledgeChunkHybridSearchRequest(
                "如何修改手机号",
                5,
                null
        );

        KnowledgeChunkVectorSearchItemVO vectorItem = vectorItem(
                3001L,
                "手机号变更可以通过账户安全页面完成",
                0.88F
        );

        when(textSearchService.searchChunks(any()))
                .thenReturn(PageResult.of(List.of(), 0, 1, 10));
        when(vectorSearchService.vectorSearch(any()))
                .thenReturn(List.of(vectorItem));

        List<KnowledgeChunkHybridSearchItemVO> results = hybridSearchService.hybridSearch(request);

        assertEquals(1, results.size());

        KnowledgeChunkHybridSearchItemVO result = results.get(0);

        assertEquals(3001L, result.chunkId());
        assertEquals("VECTOR", result.matchType());
        assertEquals(null, result.textRankScore());
        assertNotNull(result.vectorRankScore());
        assertEquals(0.88F, result.vectorRawScore());
    }

    @Test
    @DisplayName("混合检索结果应该按照最终得分倒序排列，并限制 topK")
    void shouldSortByFinalScoreDescAndLimitTopK() {
        KnowledgeChunkHybridSearchRequest request = new KnowledgeChunkHybridSearchRequest(
                "登录失败怎么办",
                2,
                null
        );

        /*
         * text: 1、2、3
         * vector: 2、4
         *
         * chunk 2 同时被全文和向量命中，有 BOTH 奖励，通常应该排在最前面。
         * 最终 topK = 2，所以只返回前两条。
         */
        when(textSearchService.searchChunks(any()))
                .thenReturn(PageResult.of(
                        List.of(
                                textItem(1L, "登录失败可能是密码错误"),
                                textItem(2L, "登录失败可能是账号被冻结"),
                                textItem(3L, "登录失败可以尝试清理缓存")
                        ),
                        3,
                        1,
                        4
                ));

        when(vectorSearchService.vectorSearch(any()))
                .thenReturn(List.of(
                        vectorItem(2L, "账号被冻结会导致登录失败", 0.91F),
                        vectorItem(4L, "验证码错误也可能导致登录失败", 0.82F)
                ));

        List<KnowledgeChunkHybridSearchItemVO> results = hybridSearchService.hybridSearch(request);

        assertEquals(2, results.size());

        assertEquals(2L, results.get(0).chunkId());
        assertEquals("BOTH", results.get(0).matchType());

        assertTrue(results.get(0).finalScore() >= results.get(1).finalScore());
    }

    @Test
    @DisplayName("chunkId 为空的检索结果应该被跳过")
    void shouldIgnoreResultWhenChunkIdIsNull() {
        KnowledgeChunkHybridSearchRequest request = new KnowledgeChunkHybridSearchRequest(
                "如何重置密码",
                5,
                null
        );

        when(textSearchService.searchChunks(any()))
                .thenReturn(PageResult.of(
                        List.of(
                                textItem(null, "这是一个异常全文检索结果"),
                                textItem(5001L, "这是一个正常全文检索结果")
                        ),
                        2,
                        1,
                        10
                ));

        when(vectorSearchService.vectorSearch(any()))
                .thenReturn(List.of(
                        vectorItem(null, "这是一个异常向量检索结果", 0.9F)
                ));

        List<KnowledgeChunkHybridSearchItemVO> results = hybridSearchService.hybridSearch(request);

        assertEquals(1, results.size());
        assertEquals(5001L, results.get(0).chunkId());
    }

    private KnowledgeChunkSearchItemVO textItem(Long chunkId, String chunkText) {
        return new KnowledgeChunkSearchItemVO(
                chunkId,
                9001L,
                1,
                chunkText,
                "测试知识文档",
                100L,
                "账号问题",
                "登录,账号",
                KnowledgeChunkStatusConstants.ACTIVE,
                chunkText == null ? 0 : chunkText.length(),
                LocalDateTime.now()
        );
    }

    private KnowledgeChunkVectorSearchItemVO vectorItem(Long chunkId, String chunkText, Float score) {
        return new KnowledgeChunkVectorSearchItemVO(
                chunkId,
                9001L,
                1,
                chunkText,
                "测试知识文档",
                100L,
                "账号问题",
                "登录,账号",
                score
        );
    }
}