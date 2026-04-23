package com.xcvk.platform.workflow.search.service;

import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.workflow.model.query.TicketManageQuery;
import com.xcvk.platform.workflow.model.vo.TicketManageListItemVO;

/**
 * 工单搜索服务接口
 *
 * <p>用于承接工单在 Elasticsearch 中的搜索能力，
 * 与基于 MySQL 的列表查询逻辑解耦。</p>
 *
 * <p>当前阶段先提供处理侧工单搜索能力，
 * 支持关键词搜索、条件过滤、分页与排序。</p>
 *
 * @author Programmer
 * @since 2026-04-23
 */
public interface TicketSearchService {

    /**
     * 处理侧工单搜索
     *
     * @param identity 当前登录身份
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<TicketManageListItemVO> searchManageTickets(CurrentLoginIdentity identity, TicketManageQuery query);
}