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
     * 意图识别
     */
    INTENT_CLASSIFY,



    /**
     * 意图分发
     */
    ASSISTANT_DISPATCH,

    /**
     * 工单范围校验
     */
    TICKET_SCOPE_VALIDATE,

    /**
     * 知识问答
     */
    KNOWLEDGE_QA,

    /**
     * 创建工单 Tool
     */
    TOOL_CREATE_TICKET,

    /**
     * 查询工单 Tool
     */
    TOOL_QUERY_TICKET,

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
     * 创建工单确认检查
     */
    TOOL_CREATE_TICKET_CONFIRM_CHECK,

    /**
     * 创建工单请求构建
     */
    TOOL_CREATE_TICKET_REQUEST_BUILD,


    /**
     * 创建工单调用 workflow-service
     */
    TOOL_CREATE_TICKET_CALL_WORKFLOW,



    /**
     * 创建工单解析 workflow 响应
     */
    TOOL_CREATE_TICKET_UNWRAP_RESPONSE,

    /**
     * 创建工单结果构建
     */
    TOOL_CREATE_TICKET_BUILD_RESULT,

    /**
     * 创建工单执行
     */
    TOOL_CREATE_TICKET_EXECUTE,

    /**
     * 查询工单请求构建
     */
    TOOL_QUERY_TICKET_REQUEST_BUILD,

    /**
     * 查询工单调用 workflow-service
     */
    TOOL_QUERY_TICKET_CALL_WORKFLOW,

    /**
     * 查询工单解析 workflow 响应
     */
    TOOL_QUERY_TICKET_UNWRAP_RESPONSE,

    /**
     * 查询工单执行
     */
    TOOL_QUERY_TICKET_EXECUTE,

    /**
     * 查询工单回答构建
     */
    TOOL_QUERY_TICKET_BUILD_ANSWER,

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
