package com.xcvk.platform.ai.trace.enums;

/**
 *  RAG 链路节点类型枚举。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-18 0:30
 */

public enum RagTraceNodeType {

    /**
     * 问题改写
     */
    QUESTION_REWRITE,

    /**
     * 检索
     */
    RETRIEVAL,

    /**
     * 原始检索
     */
    ORIGINAL_RETRIEVAL,

    /**
     * 重写检索
     */
    REWRITE_RETRIEVAL,

    /**
     * RRF融合排序
     */
    RRF_FUSION,


    /**
     * 低相关性检查
     */
    RELEVANCE_CHECK,

    /**
     * Prompt构建
     */
    PROMPT_BUILD,

    /**
     * 大模型生成
     */
    LLM_GENERATE

}
