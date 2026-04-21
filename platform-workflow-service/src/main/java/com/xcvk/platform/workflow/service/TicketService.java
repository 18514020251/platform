package com.xcvk.platform.workflow.service;

import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.workflow.model.cmd.CreateTicketCmd;
import com.xcvk.platform.workflow.model.query.MyTicketQuery;
import com.xcvk.platform.workflow.model.query.TicketManageQuery;
import com.xcvk.platform.workflow.model.vo.CreateTicketResponse;
import com.xcvk.platform.workflow.model.vo.TicketDetailVO;
import com.xcvk.platform.workflow.model.vo.TicketListItemVO;
import com.xcvk.platform.workflow.model.vo.TicketManageListItemVO;

/**
 * 工单服务接口
 *
 * <p>当前阶段先围绕工单主链提供两类能力：</p>
 * <ul>
 *     <li>员工侧：创建工单、我的工单列表、我的工单详情</li>
 *     <li>处理侧：支持人员/管理员视角的工单列表、接单</li>
 * </ul>
 *
 * <p>其中工单创建统一收口到命令对象，
 * 以保证手工创建与 AI 创建共享同一套业务规则。</p>
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

    /**
     * 分页查询处理侧工单列表
     *
     * @param identity 当前登录身份
     * @param query 查询条件
     * @return 处理侧工单分页结果
     */
    PageResult<TicketManageListItemVO> pageManageTickets(CurrentLoginIdentity identity, TicketManageQuery query);

    /**
     * 接单
     *
     * <p>接单本质上是将“待受理且未分派”的工单认领为当前处理人，
     * 并同步将工单状态推进到处理中。</p>
     *
     * <p>当前阶段不单独建接单记录表，
     * 只更新工单主表中的当前态字段；
     * 后续若增加操作轨迹与留痕，再统一扩展历史存储链路。</p>
     *
     * @param identity 当前登录身份
     * @param ticketId 工单ID
     */
    void acceptTicket(CurrentLoginIdentity identity, Long ticketId);
}