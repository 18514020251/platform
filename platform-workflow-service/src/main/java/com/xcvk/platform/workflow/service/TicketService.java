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
 *     <li>处理侧：支持人员/管理员视角的工单列表</li>
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
     * <p>该方法是 workflow 领域内统一的工单创建入口。
     * 手工创建和 AI 创建最终都应收敛到这里，
     * 避免工单创建逻辑分散到多个入口重复实现。</p>
     *
     * @param cmd 创建工单命令对象
     * @return 创建结果
     */
    CreateTicketResponse createTicket(CreateTicketCmd cmd);

    /**
     * 分页查询我的工单
     *
     * <p>该方法用于员工侧查看自己创建的工单列表，
     * 查询范围严格受创建人 ID 限制。</p>
     *
     * @param creatorId 当前登录用户ID
     * @param query 查询条件
     * @return 我的工单分页结果
     */
    PageResult<TicketListItemVO> pageMyTickets(Long creatorId, MyTicketQuery query);

    /**
     * 查询我的工单详情
     *
     * <p>该方法用于员工侧查看自己创建的工单详情，
     * 防止用户越权读取其他人的工单信息。</p>
     *
     * @param creatorId 当前登录用户ID
     * @param ticketId 工单ID
     * @return 工单详情
     */
    TicketDetailVO getMyTicketDetail(Long creatorId, Long ticketId);

    /**
     * 分页查询处理侧工单列表
     *
     * <p>该方法面向支持人员或管理员的处理工作台列表。
     * 当前阶段采用保守权限策略：</p>
     * <ul>
     *     <li>管理员：查看全部工单</li>
     *     <li>支持人员：查看分配给自己或尚未分派的工单</li>
     * </ul>
     *
     * <p>这样做是为了先打通处理侧主链，
     * 后续如需按工单类型默认处理角色做更精细的数据权限控制，
     * 再在此基础上演进。</p>
     *
     * @param identity 当前登录身份
     * @param query 查询条件
     * @return 处理侧工单分页结果
     */
    PageResult<TicketManageListItemVO> pageManageTickets(CurrentLoginIdentity identity, TicketManageQuery query);
}