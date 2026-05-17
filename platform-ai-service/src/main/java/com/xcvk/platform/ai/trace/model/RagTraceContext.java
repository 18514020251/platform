package com.xcvk.platform.ai.trace.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 *  RAG 链路追踪上下文。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-18 0:30
 */
@Data
public class RagTraceContext {

    /**
     * trace id
     */
    private String traceId;

    /**
     * 用户原问题
     */
    private String question;

    /**
     * 最终回答
     */
    private String finalAnswer;

    /**
     * SUCCESS / FAILED
     */
    private String status;

    /**
     * 开始时间
     */
    private Long startTime;

    /**
     * 结束时间
     */
    private Long endTime;

    /**
     * 总耗时
     */
    private Long totalLatencyMs;

    /**
     * 节点列表
     */
    private List<RagTraceNode> nodes = new ArrayList<>();

    public void addNode(RagTraceNode node) {

        this.nodes.add(node);

    }
}
