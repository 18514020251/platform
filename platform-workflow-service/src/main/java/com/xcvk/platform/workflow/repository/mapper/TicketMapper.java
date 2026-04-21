package com.xcvk.platform.workflow.repository.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xcvk.platform.workflow.model.entity.Ticket;
import org.apache.ibatis.annotations.Param;

/**
 * 工单 Mapper
 *
 * <p>用于访问工单主表数据。
 * 当前阶段除基础 CRUD 外，
 * 还提供处理侧需要的条件更新能力，用于保证接单等状态变更动作的并发安全。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public interface TicketMapper extends BaseMapper<Ticket> {

    /**
     * 条件接单
     *
     * <p>只有当工单仍然处于“待受理且未分派”状态时，
     * 当前更新才会成功。
     * 这样做的目的是将接单并发控制下沉到数据库原子更新层，
     * 避免先查后改带来的并发覆盖问题。</p>
     *
     * @param ticketId 工单ID
     * @param assigneeId 接单人ID
     * @param assigneeName 接单人姓名
     * @param fromStatus 接单前状态
     * @param toStatus 接单后状态
     * @return 影响行数
     */
    int acceptTicket(@Param("ticketId") Long ticketId,
                     @Param("assigneeId") Long assigneeId,
                     @Param("assigneeName") String assigneeName,
                     @Param("fromStatus") String fromStatus,
                     @Param("toStatus") String toStatus);
}