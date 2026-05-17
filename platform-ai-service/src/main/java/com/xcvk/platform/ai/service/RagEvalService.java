package com.xcvk.platform.ai.service;

import com.xcvk.platform.ai.model.dto.RagEvalDatasetCreateRequest;
import com.xcvk.platform.ai.model.dto.RagEvalRunRequest;
import com.xcvk.platform.ai.model.vo.RagEvalCaseResultVO;
import com.xcvk.platform.ai.model.vo.RagEvalDatasetVO;
import com.xcvk.platform.ai.model.vo.RagEvalRunVO;

import java.util.List;

/**
 * RAG 离线评测服务。
 */
public interface RagEvalService {

    RagEvalDatasetVO createDataset(RagEvalDatasetCreateRequest request);

    List<RagEvalDatasetVO> listDatasets(Long categoryId, Integer limit);

    RagEvalRunVO runEvaluation(RagEvalRunRequest request);

    List<RagEvalRunVO> listRuns(Integer limit);

    List<RagEvalCaseResultVO> listCaseResults(Long runId);
}