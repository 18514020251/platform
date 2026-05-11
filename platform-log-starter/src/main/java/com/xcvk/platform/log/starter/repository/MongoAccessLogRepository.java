package com.xcvk.platform.log.starter.repository;

import com.xcvk.platform.log.starter.model.AccessLogRecord;
import com.xcvk.platform.log.starter.properties.AccessLogProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;

import java.time.Duration;

/**
 * 基于 MongoDB 的访问日志存储实现。
 *
 * <p>访问日志属于高频写入、结构相对灵活的数据，使用 MongoDB 可以减少业务库写压力，
 * 也便于后续按 traceId、URL、耗时、错误类型等字段做问题排查。</p>
 *
 * <p>启动时默认创建常用查询索引和 TTL 索引，避免访问日志集合无限增长。</p>
 *
 * @author Programmer
 * @version 1.1
 * @date 2026-05-11
 */
@Slf4j
@RequiredArgsConstructor
public class MongoAccessLogRepository implements AccessLogRepository, InitializingBean {

    private final MongoTemplate mongoTemplate;

    private final AccessLogProperties properties;

    @Override
    public void afterPropertiesSet() {
        ensureIndexes();
    }

    @Override
    public void save(AccessLogRecord record) {
        mongoTemplate.insert(record, properties.getCollectionName());
        log.debug("插入访问日志文档成功, 集合={}, traceId={}, 服务={}, 请求路径={}",
                properties.getCollectionName(),
                record.traceId(),
                record.serviceName(),
                record.url());
    }

    private void ensureIndexes() {
        if (!properties.isCreateIndexes()) {
            return;
        }

        try {
            IndexOperations indexOps = mongoTemplate.indexOps(properties.getCollectionName());
            indexOps.ensureIndex(new Index()
                    .on("traceId", Sort.Direction.ASC)
                    .named("idx_access_log_trace_id"));
            indexOps.ensureIndex(new Index()
                    .on("serviceName", Sort.Direction.ASC)
                    .on("requestTime", Sort.Direction.DESC)
                    .named("idx_access_log_service_time"));
            indexOps.ensureIndex(new Index()
                    .on("success", Sort.Direction.ASC)
                    .on("requestTime", Sort.Direction.DESC)
                    .named("idx_access_log_success_time"));
            indexOps.ensureIndex(new Index()
                    .on("userId", Sort.Direction.ASC)
                    .on("requestTime", Sort.Direction.DESC)
                    .named("idx_access_log_user_time"));
            indexOps.ensureIndex(new Index()
                    .on("url", Sort.Direction.ASC)
                    .on("requestTime", Sort.Direction.DESC)
                    .named("idx_access_log_url_time"));

            if (properties.getTtlDays() > 0) {
                indexOps.ensureIndex(new Index()
                        .on("requestTime", Sort.Direction.ASC)
                        .expire(Duration.ofDays(properties.getTtlDays()))
                        .named("idx_access_log_request_time_ttl"));
            }
        } catch (Exception ex) {
            log.warn("ensure access log mongodb indexes failed, collection={}, error={}",
                    properties.getCollectionName(), ex.getMessage());
        }
    }
}
