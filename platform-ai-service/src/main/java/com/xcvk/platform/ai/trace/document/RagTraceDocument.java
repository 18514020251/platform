package com.xcvk.platform.ai.trace.document;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

/**
 *  rag 文档
 * */
@Data
@Document("rag_trace")
public class RagTraceDocument {

    @Id
    private String id;

    /**
     * trace id
     */
    private String traceId;

    /**
     * 用户问题
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
     * 节点时间线
     */
    private List<RagTraceNodeDocument> nodes;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}