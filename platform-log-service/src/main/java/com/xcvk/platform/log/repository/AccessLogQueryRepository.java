package com.xcvk.platform.log.repository;

import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.log.model.query.AccessLogPageQuery;
import com.xcvk.platform.log.model.vo.AccessLogItemVO;

import java.util.List;

/**
 * 访问日志查询仓储。
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-05-11
 */
public interface AccessLogQueryRepository {

    PageResult<AccessLogItemVO> pageAccessLogs(AccessLogPageQuery query);

    List<AccessLogItemVO> listByTraceId(String traceId);
}
