package com.xcvk.platform.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcvk.platform.workflow.model.entity.Ticket;
import com.xcvk.platform.workflow.model.entity.TicketEvent;
import com.xcvk.platform.workflow.model.vo.TicketEventVO;

import java.util.List;

/**
 * 工单操作流水服务
 */
public interface TicketEventService extends IService<TicketEvent> {

    /**
     * 记录创建工单事件。
     */
    void recordCreateEvent(Ticket ticket, Long operatorId, String operatorName);

    /**
     * 记录接单事件。
     */
    void recordAcceptEvent(Ticket ticket, Long operatorId, String operatorName);

    /**
     * 记录派单事件。
     */
    void recordAssignEvent(Ticket ticket,
                           Long operatorId,
                           String operatorName,
                           Long assigneeId,
                           String assigneeName);

    /**
     * 记录处理结果事件。
     */
    void recordStatusChangeEvent(Ticket ticket,
                                 Long operatorId,
                                 String operatorName,
                                 String targetStatus,
                                 String remark);

    /**
     * 查询指定工单的操作流水。
     */
    List<TicketEventVO> listTicketEvents(Long ticketId);
}