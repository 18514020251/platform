package com.xcvk.platform.workflow.model.query;

import java.io.Serial;
import java.io.Serializable;

/**
 * 我的工单查询对象
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public record MyTicketQuery(
        String status,
        Integer pageNum,
        Integer pageSize
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public int safePageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public int safePageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}