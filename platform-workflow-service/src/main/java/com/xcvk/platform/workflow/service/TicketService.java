package com.xcvk.platform.workflow.service;

import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.workflow.model.cmd.CreateTicketCmd;
import com.xcvk.platform.workflow.model.query.MyTicketQuery;
import com.xcvk.platform.workflow.model.vo.CreateTicketResponse;
import com.xcvk.platform.workflow.model.vo.TicketDetailVO;
import com.xcvk.platform.workflow.model.vo.TicketListItemVO;

/**
 * 工单服务接口
 *
 * <p>当前阶段先围绕员工主链提供：
 * 创建工单、我的工单列表、工单详情。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public interface TicketService {

    /**
     * 创建工单
     *
     * @param cmd 创建工单命令对象
     * @return 创建结果
     */
    CreateTicketResponse createTicket(CreateTicketCmd cmd);

    /**
     * 分页查询我的工单
     *
     * @param creatorId 当前登录用户ID
     * @param query 查询条件
     * @return 我的工单分页结果
     */
    PageResult<TicketListItemVO> pageMyTickets(Long creatorId, MyTicketQuery query);

    /**
     * 查询我的工单详情
     *
     * @param creatorId 当前登录用户ID
     * @param ticketId 工单ID
     * @return 工单详情
     */
    TicketDetailVO getMyTicketDetail(Long creatorId, Long ticketId);

}