package com.xcvk.platform.ai.trace.mapper;

import com.xcvk.platform.ai.trace.document.RagTraceDocument;
import com.xcvk.platform.ai.trace.document.RagTraceNodeDocument;
import com.xcvk.platform.ai.trace.model.RagTraceContext;
import com.xcvk.platform.ai.trace.model.RagTraceNode;

import java.time.LocalDateTime;
import java.util.List;

/**
 *  Rag Trace 文档转换
 * */
public class RagTraceDocumentMapper {

    private RagTraceDocumentMapper() {
    }

    public static RagTraceDocument toDocument(
            RagTraceContext context
    ) {

        if (context == null) {
            return null;
        }

        RagTraceDocument document = new RagTraceDocument();

        document.setTraceId(
                context.getTraceId()
        );

        document.setQuestion(
                context.getQuestion()
        );

        document.setFinalAnswer(
                context.getFinalAnswer()
        );

        document.setStatus(
                context.getStatus()
        );

        document.setStartTime(
                context.getStartTime()
        );

        document.setEndTime(
                context.getEndTime()
        );

        document.setTotalLatencyMs(
                context.getTotalLatencyMs()
        );

        document.setNodes(
                mapNodes(context.getNodes())
        );

        document.setCreatedAt(
                LocalDateTime.now()
        );

        document.setExecutionLogId(
                context.getExecutionLogId()
        );

        return document;
    }

    private static List<RagTraceNodeDocument> mapNodes(
            List<RagTraceNode> nodes
    ) {

        if (nodes == null) {
            return List.of();
        }

        return nodes.stream()
                .map(RagTraceDocumentMapper::mapNode)
                .toList();
    }

    private static RagTraceNodeDocument mapNode(
            RagTraceNode node
    ) {

        RagTraceNodeDocument document =
                new RagTraceNodeDocument();

        document.setNodeType(
                node.getNodeType()
        );

        document.setStartTime(
                node.getStartTime()
        );

        document.setEndTime(
                node.getEndTime()
        );

        document.setLatencyMs(
                node.getLatencyMs()
        );

        document.setInputSummary(
                node.getInputSummary()
        );

        document.setOutputSummary(
                node.getOutputSummary()
        );

        document.setStatus(
                node.getStatus()
        );

        document.setErrorMessage(
                node.getErrorMessage()
        );

        return document;
    }
}