package com.xcvk.platform.ai.trace.service;

import com.xcvk.platform.ai.trace.model.RagTraceContext;

/**
 *   rag 链路追踪服务
 * */
public interface RagTraceService {

    void save(RagTraceContext context);
}