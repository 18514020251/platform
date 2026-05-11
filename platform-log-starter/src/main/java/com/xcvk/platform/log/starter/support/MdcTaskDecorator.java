package com.xcvk.platform.log.starter.support;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * MDC 线程上下文装饰器。
 *
 * <p>接口访问日志采用线程池异步写入 MongoDB，默认线程切换会丢失 traceId、userId、serviceName 等 MDC 信息。
 * 该装饰器在任务提交时复制当前 MDC，并在异步线程执行期间恢复，执行完成后再还原原线程上下文。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-11
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> parentContext = MDC.getCopyOfContextMap();
        return () -> {
            Map<String, String> previousContext = MDC.getCopyOfContextMap();
            try {
                if (parentContext == null || parentContext.isEmpty()) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(parentContext);
                }
                runnable.run();
            } finally {
                if (previousContext == null || previousContext.isEmpty()) {
                    MDC.clear();
                } else {
                    MDC.setContextMap(previousContext);
                }
            }
        };
    }
}
