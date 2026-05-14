package com.xcvk.platform.workflow.service;

import com.xcvk.platform.api.contract.workflow.model.QueryAiTicketRequest;
import com.xcvk.platform.api.contract.workflow.model.QueryAiTicketResponse;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.common.domain.PageResult;
import com.xcvk.platform.workflow.model.dto.AssignTicketRequest;
import com.xcvk.platform.workflow.model.dto.UpdateTicketStatusRequest;
import com.xcvk.platform.workflow.model.query.MyTicketQuery;
import com.xcvk.platform.workflow.model.query.TicketManageQuery;
import com.xcvk.platform.workflow.model.vo.*;

import java.util.List;

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
     * @param creatorId 工单创建者ID
     * @param creatorName 工单创建者名称
     * @param ticketTypeCode 工单类型编码
     * @param title 工单标题
     * @param content  工单内容
     * @param priority 工单优先级
     * @return 创建结果
     */
    CreateTicketResponse createTicket(Long creatorId, String creatorName,
                                      String ticketTypeCode, String title,
                                      String content, String priority);

    /**
     * 分页查询我的工单
     *
     * @param creatorId 当前登录用户ID
     * @param query 查询条件
     * @return 我的工单分页结果
     */
    PageResult<TicketListItemVO> pageMyTickets(Long creatorId, MyTicketQuery query);

    /**
     * 查询工单详情。
     *
     * <p>管理员可以查看所有工单；
     * 非管理员只能查看自己创建的工单。</p>
     *
     * @param identity 当前登录身份
     * @param ticketId 工单ID
     * @return 工单详情
     */
    TicketDetailVO getMyTicketDetail(CurrentLoginIdentity identity, Long ticketId);

    /**
     * 查询处理侧工单详情。
     *
     * <p>管理员可以查看所有工单；
     * 支持人员只能查看未分派工单或自己正在处理的工单。</p>
     *
     * @param identity 当前登录身份
     * @param ticketId 工单ID
     * @return 工单详情
     */
    TicketDetailVO getManageTicketDetail(CurrentLoginIdentity identity, Long ticketId);

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

    /**
     * 更新工单状态
     *
     * <p>当前阶段该方法只负责将“处理中”的工单推进到最终处理结果，
     * 即更新为已解决或已拒绝。</p>
     *
     * <p>为避免状态更新覆盖问题，
     * 最终落库时应基于当前状态做条件更新。</p>
     *
     * @param identity 当前登录身份
     * @param ticketId 工单ID
     * @param request 更新状态请求
     */
    void updateTicketStatus(CurrentLoginIdentity identity, Long ticketId, UpdateTicketStatusRequest request);

    /**
     * 管理员派发工单
     *
     * <p>当前阶段该方法只负责将“待处理”工单分配给指定处理人，
     * 并同步将工单状态推进到处理中。</p>
     *
     * @param identity 当前登录身份
     * @param ticketId 工单ID
     * @param request  管理员指定的接单人对象
     * */
    void assignTicket(CurrentLoginIdentity identity, Long ticketId, AssignTicketRequest request);

    /**
     * AI Agent 创建工单。
     *
     * <p>该方法与手工创建工单共享同一套校验、编号生成、入库和搜索索引同步逻辑，
     * 但工单来源固定为 AI_AGENT。</p>
     *
     * @param creatorId 创建人ID
     * @param creatorName 创建人名称
     * @param ticketTypeCode 工单类型编码
     * @param title 工单标题
     * @param content 工单内容
     * @param priority 优先级
     * @param sourceRef AI会话ID / Agent执行ID
     * @return 创建结果
     */
    CreateTicketResponse createAiTicket(Long creatorId, String creatorName,
                                        String ticketTypeCode, String title,
                                        String content, String priority,
                                        String sourceRef);

    /**
     * AI Agent 搜索工单
     *
     * @param request 搜索条件
     * @return 工单列表
     */
    QueryAiTicketResponse queryTicketByAi(QueryAiTicketRequest request);

    /**
     * 查询工单操作流水。
     *
     * @param identity 当前登录身份
     * @param ticketId 工单ID
     * @return 操作流水列表
     */
    List<TicketEventVO> listTicketEvents(CurrentLoginIdentity identity, Long ticketId);

    /**
     * 同步工单到搜索索引。
     *
     * <p>该方法由搜索同步任务处理器调用，失败时应抛出异常，
     * 由任务处理器负责重试和状态记录。</p>
     *
     * @param ticketId 工单ID
     */
    void syncTicketToSearchIndex(Long ticketId);

}