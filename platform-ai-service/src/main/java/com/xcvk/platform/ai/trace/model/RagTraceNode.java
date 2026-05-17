package com.xcvk.platform.ai.trace.model;

import com.xcvk.platform.ai.trace.context.RagTraceHolder;
import lombok.Data;

/**
 *  RAG 链路节点。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-18 0:30
 */
@Data
public class RagTraceNode {

    /**
     * 节点类型
     */
    private String nodeType;

    /**
     * 开始时间
     */
    private Long startTime;

    /**
     * 结束时间
     */
    private Long endTime;

    /**
     * 耗时
     */
    private Long latencyMs;

    /**
     * 输入摘要
     */
    private Object inputSummary;

    /**
     * 输出摘要
     */
    private Object outputSummary;

    /**
     * SUCCESS / FAILED
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    RagTraceContext context = RagTraceHolder.get();
}


