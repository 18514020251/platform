package com.xcvk.platform.ai.service;

import com.xcvk.platform.ai.model.entity.AiAgentExecutionLog;

/**
 * Agent 执行日志服务。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-09
 */
public interface AgentExecutionLogService {

    /**
     * 安全保存 Agent 执行日志。
     *
     * <p>日志写入失败不应影响主业务流程。</p>
     *
     * @param executionLog 执行日志
     */
    void saveSafely(AiAgentExecutionLog executionLog);
}