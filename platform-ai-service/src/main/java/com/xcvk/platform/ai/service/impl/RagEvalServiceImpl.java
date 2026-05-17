package com.xcvk.platform.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xcvk.platform.ai.model.dto.RagEvalDatasetCreateRequest;
import com.xcvk.platform.ai.model.dto.RagEvalRunRequest;
import com.xcvk.platform.ai.model.entity.RagEvalCaseResult;
import com.xcvk.platform.ai.model.entity.RagEvalDataset;
import com.xcvk.platform.ai.model.entity.RagEvalRun;
import com.xcvk.platform.ai.model.vo.RagEvalCaseResultVO;
import com.xcvk.platform.ai.model.vo.RagEvalDatasetVO;
import com.xcvk.platform.ai.model.vo.RagEvalRunVO;
import com.xcvk.platform.ai.repository.mapper.RagEvalCaseResultMapper;
import com.xcvk.platform.ai.repository.mapper.RagEvalDatasetMapper;
import com.xcvk.platform.ai.repository.mapper.RagEvalRunMapper;
import com.xcvk.platform.ai.service.RagEvalService;
import com.xcvk.platform.ai.service.eval.RagEvalMetricCalculator;
import com.xcvk.platform.ai.service.rag.RagContextRetrievalService;
import com.xcvk.platform.ai.service.rag.RagPromptBuilder;
import com.xcvk.platform.ai.service.rag.RagQuestionRewriteService;
import com.xcvk.platform.api.contract.knowledge.model.KnowledgeRagContextItem;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.id.generator.SnowflakeIdGenerator;
import dev.langchain4j.model.chat.ChatModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

/**
 * RAG 离线评测服务实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RagEvalServiceImpl implements RagEvalService {

    private static final int DEFAULT_LIST_LIMIT = 50;

    private static final int MAX_LIST_LIMIT = 200;

    private static final int METRIC_TOP_1 = 1;

    private static final int METRIC_TOP_3 = 3;

    private static final int METRIC_TOP_5 = 5;

    private final RagEvalDatasetMapper datasetMapper;

    private final RagEvalRunMapper runMapper;

    private final RagEvalCaseResultMapper caseResultMapper;

    private final RagQuestionRewriteService questionRewriteService;

    private final RagContextRetrievalService contextRetrievalService;

    private final RagPromptBuilder promptBuilder;

    private final ChatModel chatModel;

    private final SnowflakeIdGenerator idGenerator;

    @Override
    public RagEvalDatasetVO createDataset(RagEvalDatasetCreateRequest request) {
        validateCreateDatasetRequest(request);

        RagEvalDataset dataset = new RagEvalDataset()
                .setId(idGenerator.nextId())
                .setQuestion(request.question().trim())
                .setExpectedChunkIds(joinIds(request.expectedChunkIds()))
                .setExpectedAnswer(safeTrim(request.expectedAnswer()))
                .setCategoryId(request.categoryId())
                .setDifficulty(resolveDifficulty(request.difficulty()));

        datasetMapper.insert(dataset);

        return RagEvalDatasetVO.from(dataset, request.expectedChunkIds());
    }

    @Override
    public List<RagEvalDatasetVO> listDatasets(Long categoryId, Integer limit) {
        LambdaQueryWrapper<RagEvalDataset> wrapper = new LambdaQueryWrapper<RagEvalDataset>()
                .eq(categoryId != null, RagEvalDataset::getCategoryId, categoryId)
                .orderByDesc(RagEvalDataset::getCreatedAt)
                .last("LIMIT " + safeListLimit(limit));

        return datasetMapper.selectList(wrapper)
                .stream()
                .map(dataset -> RagEvalDatasetVO.from(dataset, parseIds(dataset.getExpectedChunkIds())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RagEvalRunVO runEvaluation(RagEvalRunRequest request) {
        RagEvalRunRequest safeRequest = request == null
                ? new RagEvalRunRequest(null, null, null, null, null)
                : request;

        List<RagEvalDataset> datasets = loadDatasets(safeRequest);
        BizAssert.isTrue(!datasets.isEmpty(), ErrorCode.BIZ_ERROR, "当前没有可评测的数据集样本");

        Long runId = idGenerator.nextId();
        List<RagEvalCaseResult> caseResults = new ArrayList<>(datasets.size());

        for (RagEvalDataset dataset : datasets) {
            caseResults.add(evaluateCase(runId, dataset, safeRequest));
        }

        caseResults.forEach(caseResultMapper::insert);

        RagEvalRun run = buildRunSummary(runId, caseResults, safeRequest);
        runMapper.insert(run);

        log.info("RAG离线评测完成，runId={}, datasetSize={}, recallAt5={}, mrr={}, avgRetrieveLatencyMs={}",
                runId,
                run.getDatasetSize(),
                run.getRecallAt5(),
                run.getMrr(),
                run.getAvgRetrieveLatencyMs()
        );

        return RagEvalRunVO.from(run);
    }

    @Override
    public List<RagEvalRunVO> listRuns(Integer limit) {
        LambdaQueryWrapper<RagEvalRun> wrapper = new LambdaQueryWrapper<RagEvalRun>()
                .orderByDesc(RagEvalRun::getCreatedAt)
                .last("LIMIT " + safeListLimit(limit));

        return runMapper.selectList(wrapper)
                .stream()
                .map(RagEvalRunVO::from)
                .toList();
    }

    @Override
    public List<RagEvalCaseResultVO> listCaseResults(Long runId) {
        BizAssert.notNull(runId, ErrorCode.PARAM_INVALID, "评测任务ID不能为空");

        LambdaQueryWrapper<RagEvalCaseResult> wrapper = new LambdaQueryWrapper<RagEvalCaseResult>()
                .eq(RagEvalCaseResult::getRunId, runId)
                .orderByAsc(RagEvalCaseResult::getId);

        return caseResultMapper.selectList(wrapper)
                .stream()
                .map(result -> RagEvalCaseResultVO.from(
                        result,
                        parseIds(result.getExpectedChunkIds()),
                        parseIds(result.getRetrievedChunkIds())
                ))
                .toList();
    }

    /**
     *  加载数据集样本
     * */
    private List<RagEvalDataset> loadDatasets(RagEvalRunRequest request) {
        LambdaQueryWrapper<RagEvalDataset> wrapper = new LambdaQueryWrapper<RagEvalDataset>()
                .eq(request.categoryId() != null, RagEvalDataset::getCategoryId, request.categoryId())
                .orderByAsc(RagEvalDataset::getId)
                .last("LIMIT " + request.safeDatasetLimit());

        return datasetMapper.selectList(wrapper);
    }

    private RagEvalCaseResult evaluateCase(Long runId, RagEvalDataset dataset, RagEvalRunRequest request) {
        String originalQuestion = dataset.getQuestion();
        String rewrittenQuestion = questionRewriteService.rewriteQuestion(originalQuestion);
        int retrieveTopK = request.safeRetrieveTopK();

        long retrieveStart = System.nanoTime();
        List<KnowledgeRagContextItem> contexts = contextRetrievalService.retrieveEnhancedContexts(
                originalQuestion,
                rewrittenQuestion,
                retrieveTopK,
                resolveCaseCategoryId(dataset, request)
        );
        long retrieveLatencyMs = elapsedMillis(retrieveStart);

        List<Long> expectedIds = parseIds(dataset.getExpectedChunkIds());
        List<Long> retrievedIds = contexts.stream()
                .map(KnowledgeRagContextItem::chunkId)
                .filter(Objects::nonNull)
                .toList();

        boolean hitAt1 = RagEvalMetricCalculator.hitAtK(expectedIds, retrievedIds, METRIC_TOP_1);
        boolean hitAt3 = RagEvalMetricCalculator.hitAtK(expectedIds, retrievedIds, METRIC_TOP_3);
        boolean hitAt5 = RagEvalMetricCalculator.hitAtK(expectedIds, retrievedIds, METRIC_TOP_5);
        double reciprocalRank = RagEvalMetricCalculator.reciprocalRank(expectedIds, retrievedIds);
        double contextPrecisionAt5 = RagEvalMetricCalculator.contextPrecisionAtK(expectedIds, retrievedIds, METRIC_TOP_5);

        String answer = null;
        Double faithfulnessScore = null;
        Double relevanceScore = null;
        long answerLatencyMs = 0L;

        if (request.safeGenerationEnabled()) {
            long answerStart = System.nanoTime();
            answer = generateAnswerSafely(originalQuestion, rewrittenQuestion, contexts);
            if (request.safeJudgeEnabled() && StringUtils.hasText(answer)) {
                faithfulnessScore = judgeFaithfulnessSafely(originalQuestion, answer, contexts);
                relevanceScore = judgeAnswerRelevanceSafely(originalQuestion, answer);
            }
            answerLatencyMs = elapsedMillis(answerStart);
        }

        return new RagEvalCaseResult()
                .setId(idGenerator.nextId())
                .setRunId(runId)
                .setDatasetId(dataset.getId())
                .setQuestion(originalQuestion)
                .setExpectedChunkIds(dataset.getExpectedChunkIds())
                .setRetrievedChunkIds(joinIds(retrievedIds))
                .setHitAt1(hitAt1)
                .setHitAt3(hitAt3)
                .setHitAt5(hitAt5)
                .setReciprocalRank(round4(reciprocalRank))
                .setContextPrecisionAt5(round4(contextPrecisionAt5))
                .setAnswer(answer)
                .setFaithfulnessScore(faithfulnessScore)
                .setRelevanceScore(relevanceScore)
                .setRetrieveLatencyMs(retrieveLatencyMs)
                .setAnswerLatencyMs(answerLatencyMs);
    }

    private RagEvalRun buildRunSummary(Long runId, List<RagEvalCaseResult> caseResults, RagEvalRunRequest request) {
        int datasetSize = caseResults.size();

        double recallAt1 = averageBoolean(caseResults.stream().map(RagEvalCaseResult::getHitAt1).toList());
        double recallAt3 = averageBoolean(caseResults.stream().map(RagEvalCaseResult::getHitAt3).toList());
        double recallAt5 = averageBoolean(caseResults.stream().map(RagEvalCaseResult::getHitAt5).toList());
        double mrr = averageDouble(caseResults.stream().map(RagEvalCaseResult::getReciprocalRank).toList())
                .orElse(0.0D);
        double contextPrecisionAt5 = averageDouble(caseResults.stream()
                .map(RagEvalCaseResult::getContextPrecisionAt5)
                .toList())
                .orElse(0.0D);
        Double avgFaithfulnessScore = averageDoubleNullable(caseResults.stream()
                .map(RagEvalCaseResult::getFaithfulnessScore)
                .toList());
        Double avgRelevanceScore = averageDoubleNullable(caseResults.stream()
                .map(RagEvalCaseResult::getRelevanceScore)
                .toList());
        double avgRetrieveLatencyMs = averageLong(caseResults.stream()
                .map(RagEvalCaseResult::getRetrieveLatencyMs)
                .toList())
                .orElse(0.0D);
        double avgAnswerLatencyMs = averageLong(caseResults.stream()
                .map(RagEvalCaseResult::getAnswerLatencyMs)
                .toList())
                .orElse(0.0D);

        return new RagEvalRun()
                .setId(runId)
                .setDatasetSize(datasetSize)
                .setRetrieveTopK(request.safeRetrieveTopK())
                .setCategoryId(request.categoryId())
                .setGenerationEnabled(request.safeGenerationEnabled())
                .setJudgeEnabled(request.safeJudgeEnabled())
                .setRecallAt1(round4(recallAt1))
                .setRecallAt3(round4(recallAt3))
                .setRecallAt5(round4(recallAt5))
                .setMrr(round4(mrr))
                .setContextPrecisionAt5(round4(contextPrecisionAt5))
                .setAvgFaithfulnessScore(roundNullable(avgFaithfulnessScore))
                .setAvgRelevanceScore(roundNullable(avgRelevanceScore))
                .setAvgRetrieveLatencyMs(round4(avgRetrieveLatencyMs))
                .setAvgAnswerLatencyMs(round4(avgAnswerLatencyMs));
    }

    private String generateAnswerSafely(String originalQuestion,
                                        String rewrittenQuestion,
                                        List<KnowledgeRagContextItem> contexts) {
        if (CollectionUtils.isEmpty(contexts)) {
            return null;
        }

        try {
            String prompt = promptBuilder.buildEnhancedAnswerPrompt(originalQuestion, rewrittenQuestion, contexts);
            return chatModel.chat(prompt);
        } catch (Exception e) {
            log.warn("RAG评测生成答案失败，question={}", originalQuestion, e);
            return null;
        }
    }

    private Double judgeFaithfulnessSafely(String question, String answer, List<KnowledgeRagContextItem> contexts) {
        if (CollectionUtils.isEmpty(contexts)) {
            return null;
        }

        String contextText = contexts.stream()
                .map(item -> "[chunkId=" + item.chunkId() + "] " + item.content())
                .toList()
                .toString();

        String prompt = "你是RAG评测器。请判断回答是否被给定上下文支持。"
                + "只返回0到1之间的小数，不要解释。\n"
                + "问题：" + question + "\n"
                + "上下文：" + contextText + "\n"
                + "回答：" + answer;

        return judgeScoreSafely(prompt);
    }

    private Double judgeAnswerRelevanceSafely(String question, String answer) {
        String prompt = "你是RAG评测器。请判断回答是否正面回答了用户问题。"
                + "只返回0到1之间的小数，不要解释。\n"
                + "问题：" + question + "\n"
                + "回答：" + answer;

        return judgeScoreSafely(prompt);
    }

    private Double judgeScoreSafely(String prompt) {
        try {
            String raw = chatModel.chat(prompt);
            return parseJudgeScore(raw);
        } catch (Exception e) {
            log.warn("LLM-as-Judge评分失败", e);
            return null;
        }
    }

    private Double parseJudgeScore(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }

        String cleaned = raw.trim();
        int start = -1;
        int end = -1;
        for (int i = 0; i < cleaned.length(); i++) {
            char ch = cleaned.charAt(i);
            if ((ch >= '0' && ch <= '9') || ch == '.') {
                if (start < 0) {
                    start = i;
                }
                end = i;
            } else if (start >= 0) {
                break;
            }
        }

        if (start < 0 || end < start) {
            return null;
        }

        try {
            double score = Double.parseDouble(cleaned.substring(start, end + 1));
            if (score < 0.0D) {
                return 0.0D;
            }
            if (score > 1.0D) {
                return 1.0D;
            }
            return round4(score);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long resolveCaseCategoryId(RagEvalDataset dataset, RagEvalRunRequest request) {
        if (request.categoryId() != null) {
            return request.categoryId();
        }
        return dataset.getCategoryId();
    }

    /**
     *  创建评测样本请求参数校验
     * */
    private void validateCreateDatasetRequest(RagEvalDatasetCreateRequest request) {
        BizAssert.notNull(request, ErrorCode.PARAM_INVALID, "评测样本不能为空");
        BizAssert.hasText(request.question(), ErrorCode.PARAM_INVALID, "问题不能为空");
        BizAssert.isTrue(!CollectionUtils.isEmpty(request.expectedChunkIds()), ErrorCode.PARAM_INVALID, "标准chunk不能为空");
    }

    /**
     *  安全列表限制
     * */
    private int safeListLimit(Integer limit) {
        if (limit == null || limit < 1) {
            return DEFAULT_LIST_LIMIT;
        }
        return Math.min(limit, MAX_LIST_LIMIT);
    }

    private String resolveDifficulty(String difficulty) {
        if (!StringUtils.hasText(difficulty)) {
            return "MEDIUM";
        }
        String upper = difficulty.trim().toUpperCase();
        if ("EASY".equals(upper) || "MEDIUM".equals(upper) || "HARD".equals(upper)) {
            return upper;
        }
        return "MEDIUM";
    }

    private String safeTrim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private List<Long> parseIds(String ids) {
        if (!StringUtils.hasText(ids)) {
            return List.of();
        }

        List<Long> result = new ArrayList<>();
        for (String part : ids.split(",")) {
            String value = part == null ? "" : part.trim();
            if (!StringUtils.hasText(value)) {
                continue;
            }
            try {
                result.add(Long.parseLong(value));
            } catch (NumberFormatException e) {
                log.warn("RAG评测chunkId解析失败，value={}", value);
            }
        }
        return result;
    }

    private String joinIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return "";
        }
        return ids.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
    }

    private long elapsedMillis(long startNano) {
        return Math.max(0L, (System.nanoTime() - startNano) / 1_000_000L);
    }

    private double averageBoolean(List<Boolean> values) {
        if (values == null || values.isEmpty()) {
            return 0.0D;
        }
        long count = values.stream().filter(Boolean.TRUE::equals).count();
        return count * 1.0D / values.size();
    }

    private OptionalDouble averageDouble(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return OptionalDouble.empty();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average();
    }

    private Double averageDoubleNullable(List<Double> values) {
        OptionalDouble average = averageDouble(values);
        return average.isPresent() ? average.getAsDouble() : null;
    }

    private OptionalDouble averageLong(List<Long> values) {
        if (values == null || values.isEmpty()) {
            return OptionalDouble.empty();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .average();
    }

    private Double roundNullable(Double value) {
        return value == null ? null : round4(value);
    }

    private double round4(double value) {
        return Math.round(value * 10_000.0D) / 10_000.0D;
    }
}