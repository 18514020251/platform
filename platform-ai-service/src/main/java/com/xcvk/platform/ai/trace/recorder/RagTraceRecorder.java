package com.xcvk.platform.ai.trace.recorder;

import com.xcvk.platform.ai.trace.enums.RagTraceNodeType;
import com.xcvk.platform.ai.trace.model.RagTraceContext;
import com.xcvk.platform.ai.trace.model.RagTraceNode;

import java.util.function.Supplier;

/**
 *  RAG 链路追踪记录器
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-18 0:31
 */
public class RagTraceRecorder {

    private static final String SUCCESS = "SUCCESS";
    private static final String FAILED = "FAILED";
    private static final int ERROR_LENGTH = 2048;

    private final RagTraceContext context;


    public RagTraceRecorder(RagTraceContext context) {
        this.context = context;
    }

    public <T> T executeNode(
            RagTraceNodeType nodeType,
            Supplier<T> supplier
    ) {

        RagTraceNode node = new RagTraceNode();

        long start = System.currentTimeMillis();
        node.setStartTime(start);
        node.setNodeType(nodeType.name());

        try {
            T result = supplier.get();

            node.setOutputSummary(result);

            node.setStatus(SUCCESS);

            return result;

        } catch (Exception e) {

            node.setStatus(FAILED);

            String fullMessage = e.toString();

            node.setErrorMessage(errorMessageInterception(fullMessage));

            throw e;

        } finally {

            long end = System.currentTimeMillis();
            node.setEndTime(end);
            node.setLatencyMs(end - start);

            context.addNode(node);
        }
    }

    /**
     * 避免错误信息过长导致链路追踪数据量过大，截取前 ERROR_LENGTH 个字符
     * */
    private String errorMessageInterception(String message) {
        return message.length() > ERROR_LENGTH ? message.substring(0, ERROR_LENGTH) : message;
    }
}
