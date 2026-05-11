package com.xcvk.platform.log.service.impl;

import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.log.model.query.AccessLogPageQuery;
import com.xcvk.platform.log.model.vo.AccessLogItemVO;
import com.xcvk.platform.log.repository.AccessLogQueryRepository;
import com.xcvk.platform.log.service.AccessLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 访问日志查询服务实现。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-11
 */
@Service
@RequiredArgsConstructor
public class AccessLogQueryServiceImpl implements AccessLogQueryService {

    private final AccessLogQueryRepository accessLogQueryRepository;

    @Override
    public PageResult<AccessLogItemVO> pageAccessLogs(AccessLogPageQuery query) {
        return accessLogQueryRepository.pageAccessLogs(query);
    }

    @Override
    public List<AccessLogItemVO> listByTraceId(String traceId) {
        return accessLogQueryRepository.listByTraceId(traceId);
    }
}
