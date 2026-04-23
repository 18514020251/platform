package com.xcvk.platform.workflow.assembler;

import com.xcvk.platform.workflow.model.entity.Ticket;
import com.xcvk.platform.workflow.model.vo.TicketDetailVO;
import com.xcvk.platform.workflow.model.vo.TicketListItemVO;
import com.xcvk.platform.workflow.model.vo.TicketManageListItemVO;
import org.springframework.stereotype.Component;

/**
 * 工单对象装配器
 *
 * <p>用于统一承接工单领域中的对象转换逻辑，
 * 包括实体到列表项、详情视图对象等转换，
 * 避免 Service 层混入过多视图装配细节。</p>
 *
 * <p>当前阶段优先收敛以下转换能力：</p>
 * <ul>
 *     <li>Ticket -> TicketListItemVO</li>
 *     <li>Ticket -> TicketManageListItemVO</li>
 *     <li>Ticket -> TicketDetailVO</li>
 * </ul>
 *
 * @author Programmer
 * @since 2026-04-23
 */
@Component
public class TicketAssembler {

    /**
     * 将工单实体转换为员工侧列表项视图对象。
     *
     * @param ticket 工单实体
     * @return 员工侧工单列表项
     */
    public TicketListItemVO toTicketListItemVO(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        return new TicketListItemVO(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getTicketTypeCode(),
                ticket.getTicketTypeName(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getSource(),
                ticket.getAssigneeName(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    /**
     * 将工单实体转换为处理侧列表项视图对象。
     *
     * @param ticket 工单实体
     * @return 处理侧工单列表项
     */
    public TicketManageListItemVO toTicketManageListItemVO(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        return new TicketManageListItemVO(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getTicketTypeCode(),
                ticket.getTicketTypeName(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getSource(),
                ticket.getCreatorId(),
                ticket.getCreatorName(),
                ticket.getAssigneeId(),
                ticket.getAssigneeName(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    /**
     * 将工单实体转换为详情视图对象。
     *
     * @param ticket 工单实体
     * @return 工单详情视图对象
     */
    public TicketDetailVO toTicketDetailVO(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        return new TicketDetailVO(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getTicketTypeId(),
                ticket.getTicketTypeCode(),
                ticket.getTicketTypeName(),
                ticket.getTitle(),
                ticket.getContent(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getSource(),
                ticket.getSourceRef(),
                ticket.getCreatorId(),
                ticket.getCreatorName(),
                ticket.getAssigneeId(),
                ticket.getAssigneeName(),
                ticket.getStatusRemark(),
                ticket.getClosedAt(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}