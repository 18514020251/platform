package com.xcvk.platform.ai.model.vo.trace;

import java.util.List;

/**
 *  Rag 跟踪时间线
 * */
public record RagTraceTimelineVO(

        String traceId,

        Long executionLogId,

        String question,

        String finalAnswer,

        String status,

        Long totalLatencyMs,

        List<RagTraceTimelineNodeVO> nodes

) {
}