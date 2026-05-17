package com.xcvk.platform.ai.trace.document;

import lombok.Data;

/**
 *  节点
 * */
@Data
public class RagTraceNodeDocument {

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
}