package com.xcvk.platform.log.repository;

import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.log.model.query.AccessLogPageQuery;
import com.xcvk.platform.log.model.vo.AccessLogItemVO;
import com.xcvk.platform.log.starter.model.AccessLogRecord;
import com.xcvk.platform.log.starter.properties.AccessLogProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 基于 MongoDB 的访问日志查询仓储。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-11
 */
@Repository
@RequiredArgsConstructor
public class MongoAccessLogQueryRepository implements AccessLogQueryRepository {

    private final MongoTemplate mongoTemplate;

    private final AccessLogProperties properties;

    @Override
    public PageResult<AccessLogItemVO> pageAccessLogs(AccessLogPageQuery query) {
        Query mongoQuery = buildQuery(query);
        long total = mongoTemplate.count(mongoQuery, AccessLogRecord.class, properties.getCollectionName());

        long pageNum = query.safePageNum();
        long pageSize = query.safePageSize();
        Query pageQuery = buildQuery(query)
                .with(PageRequest.of((int) pageNum - 1, (int) pageSize,
                        Sort.by(Sort.Direction.DESC, "requestTime")));

        List<AccessLogItemVO> records = mongoTemplate
                .find(pageQuery, AccessLogRecord.class, properties.getCollectionName())
                .stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(records, total, pageNum, pageSize);
    }

    @Override
    public List<AccessLogItemVO> listByTraceId(String traceId) {
        Query query = new Query(Criteria.where("traceId").is(traceId))
                .with(Sort.by(Sort.Direction.ASC, "requestTime"));

        return mongoTemplate
                .find(query, AccessLogRecord.class, properties.getCollectionName())
                .stream()
                .map(this::toVO)
                .toList();
    }

    private Query buildQuery(AccessLogPageQuery query) {
        List<Criteria> criteriaList = new ArrayList<>();
        addEquals(criteriaList, "traceId", query.traceId());
        addEquals(criteriaList, "serviceName", query.serviceName());
        addEquals(criteriaList, "userId", query.userId());

        if (StringUtils.hasText(query.url())) {
            criteriaList.add(Criteria.where("url").regex(Pattern.quote(query.url())));
        }
        if (query.success() != null) {
            criteriaList.add(Criteria.where("success").is(query.success()));
        }
        Criteria timeCriteria = buildTimeCriteria(query.startTime(), query.endTime());
        if (timeCriteria != null) {
            criteriaList.add(timeCriteria);
        }

        Query mongoQuery = new Query();
        if (!criteriaList.isEmpty()) {
            mongoQuery.addCriteria(new Criteria().andOperator(criteriaList.toArray(Criteria[]::new)));
        }
        return mongoQuery;
    }

    private void addEquals(List<Criteria> criteriaList, String field, String value) {
        if (StringUtils.hasText(value)) {
            criteriaList.add(Criteria.where(field).is(value));
        }
    }

    private Criteria buildTimeCriteria(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null && endTime == null) {
            return null;
        }

        Criteria criteria = Criteria.where("requestTime");
        if (startTime != null) {
            criteria.gte(startTime.atZone(ZoneId.systemDefault()).toInstant());
        }
        if (endTime != null) {
            criteria.lte(endTime.atZone(ZoneId.systemDefault()).toInstant());
        }
        return criteria;
    }

    private AccessLogItemVO toVO(AccessLogRecord record) {
        return new AccessLogItemVO(
                record.id(),
                record.traceId(),
                record.serviceName(),
                record.operation(),
                record.httpMethod(),
                record.url(),
                record.classMethod(),
                record.clientIp(),
                record.userAgent(),
                record.userId(),
                record.success(),
                record.errorType(),
                record.errorMessage(),
                record.costMs(),
                record.requestTime(),
                record.createdAt()
        );
    }
}
