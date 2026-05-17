package com.xcvk.platform.ai.trace.mapper;

import com.xcvk.platform.ai.model.vo.trace.RagTraceTimelineNodeVO;
import com.xcvk.platform.ai.model.vo.trace.RagTraceTimelineVO;
import com.xcvk.platform.ai.trace.document.RagTraceDocument;
import com.xcvk.platform.ai.trace.document.RagTraceNodeDocument;

import java.util.List;

/**
 *   rag 轨迹时间线映射
 * */
public class RagTraceTimelineMapper {

    private RagTraceTimelineMapper() {
    }

    public static RagTraceTimelineVO toTimeline(
            RagTraceDocument document
    ) {

        if (document == null) {
            return null;
        }

        return new RagTraceTimelineVO(

                document.getTraceId(),

                document.getExecutionLogId(),

                document.getQuestion(),

                document.getFinalAnswer(),

                document.getStatus(),

                document.getTotalLatencyMs(),

                mapNodes(document.getNodes())
        );
    }

    private static List<RagTraceTimelineNodeVO> mapNodes(
            List<RagTraceNodeDocument> nodes
    ) {

        if (nodes == null) {
            return List.of();
        }

        return nodes.stream()
                .map(RagTraceTimelineMapper::mapNode)
                .toList();
    }

    private static RagTraceTimelineNodeVO mapNode(
            RagTraceNodeDocument node
    ) {

        return new RagTraceTimelineNodeVO(

                node.getNodeType(),

                node.getStatus(),

                node.getLatencyMs(),

                node.getStartTime(),

                node.getEndTime(),

                node.getInputSummary(),

                node.getOutputSummary(),

                node.getErrorMessage()
        );
    }
}