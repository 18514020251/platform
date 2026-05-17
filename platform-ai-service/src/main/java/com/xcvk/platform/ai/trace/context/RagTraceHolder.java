package com.xcvk.platform.ai.trace.context;

import com.xcvk.platform.ai.trace.model.RagTraceContext;

/**
 *  RAG 链路追踪上下文持有者
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-18 0:31
 */
public class RagTraceHolder {

    private static final ThreadLocal<RagTraceContext> HOLDER =
            new ThreadLocal<>();

    public static void set(RagTraceContext context) {
        HOLDER.set(context);
    }

    public static RagTraceContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }
}
