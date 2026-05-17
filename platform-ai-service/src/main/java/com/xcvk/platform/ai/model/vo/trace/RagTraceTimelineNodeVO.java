package com.xcvk.platform.ai.model.vo.trace;

/**
 * 节点信息 vo
 * */
public record RagTraceTimelineNodeVO(

        String nodeType,

        String status,

        Long latencyMs,

        Long startTime,

        Long endTime,

        Object inputSummary,

        Object outputSummary,

        String errorMessage

) {
}