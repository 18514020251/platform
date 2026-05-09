package com.xcvk.platform.ai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcvk.platform.ai.model.entity.AiAgentExecutionLog;
import com.xcvk.platform.ai.repository.mapper.AiAgentExecutionLogMapper;
import com.xcvk.platform.ai.service.AgentExecutionLogService;
import com.xcvk.platform.id.generator.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Agent 执行日志服务实现。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-09
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AgentExecutionLogServiceImpl
        extends ServiceImpl<AiAgentExecutionLogMapper, AiAgentExecutionLog>
        implements AgentExecutionLogService {

    private final SnowflakeIdGenerator idGenerator;

    @Override
    public void saveSafely(AiAgentExecutionLog executionLog) {
        if (executionLog == null) {
            return;
        }

        try {
            if (executionLog.getId() == null) {
                executionLog.setId(idGenerator.nextId());
            }

            baseMapper.insert(executionLog);
        } catch (Exception ex) {
            log.warn("保存Agent执行日志失败，question={}, intent={}, status={}",
                    executionLog.getQuestion(),
                    executionLog.getIntent(),
                    executionLog.getExecutionStatus(),
                    ex
            );
        }
    }
}