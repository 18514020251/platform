package com.xcvk.platform.log.starter.support;

import com.xcvk.platform.log.starter.model.AccessLogRecord;
import com.xcvk.platform.log.starter.repository.AccessLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 访问日志发布器。
 *
 * <p>接口主流程只负责构造日志对象，具体写入动作投递到线程池异步执行，
 * 避免 MongoDB 写入抖动影响业务接口响应。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-11
 */
@Slf4j
@RequiredArgsConstructor
public class AccessLogPublisher {

    private final AccessLogRepository repository;

    private final ThreadPoolTaskExecutor executor;

    /**
     * 异步保存访问日志。
     *
     * @param record 访问日志记录
     */
    public void publish(AccessLogRecord record) {
        try {
            executor.execute(() -> {
                try {
                    repository.save(record);
                    log.debug("保存访问日志到MongoDB成功, traceId={}, 服务={}, 请求路径={}, 成功={}, 耗时={}ms",
                            record.traceId(),
                            record.serviceName(),
                            record.url(),
                            record.success(),
                            record.costMs());
                } catch (Exception ex) {
                    log.warn("保存访问日志到MongoDB失败, traceId={}, 服务={}, 请求路径={}, 错误={}",
                            record.traceId(),
                            record.serviceName(),
                            record.url(),
                            ex.getMessage(),
                            ex);
                }
            });
        } catch (Exception ex) {
            log.warn("提交访问日志异步任务失败, traceId={}, 服务={}, 请求路径={}, 错误={}",
                    record.traceId(),
                    record.serviceName(),
                    record.url(),
                    ex.getMessage(),
                    ex);
        }
    }
}
