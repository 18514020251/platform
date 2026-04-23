package com.xcvk.platform.workflow.search.assembler;

import com.xcvk.platform.workflow.model.entity.Ticket;
import com.xcvk.platform.workflow.search.model.index.TicketIndex;
import org.springframework.stereotype.Component;

/**
 * 工单搜索对象装配器
 *
 * <p>用于统一承接工单领域对象到 Elasticsearch 索引对象的转换逻辑，
 * 避免将搜索模型构建细节混入 Service 层或普通视图装配器中。</p>
 *
 * <p>当前阶段主要负责：</p>
 * <ul>
 *     <li>Ticket -> TicketIndex</li>
 * </ul>
 *
 * @author Programmer
 * @since 2026-04-23
 */
@Component
public class TicketSearchAssembler {

    /**
     * 将工单实体转换为工单搜索索引对象。
     *
     * @param ticket 工单实体
     * @return 工单搜索索引对象
     */
    public TicketIndex toIndex(Ticket ticket) {
        if (ticket == null) {
            return null;
        }

        return new TicketIndex()
                .setId(ticket.getId())
                .setTicketNo(ticket.getTicketNo())
                .setTicketTypeId(ticket.getTicketTypeId())
                .setTicketTypeCode(ticket.getTicketTypeCode())
                .setTicketTypeName(ticket.getTicketTypeName())
                .setTitle(ticket.getTitle())
                .setContent(ticket.getContent())
                .setStatus(ticket.getStatus())
                .setPriority(ticket.getPriority())
                .setSource(ticket.getSource())
                .setSourceRef(ticket.getSourceRef())
                .setCreatorId(ticket.getCreatorId())
                .setCreatorName(ticket.getCreatorName())
                .setAssigneeId(ticket.getAssigneeId())
                .setAssigneeName(ticket.getAssigneeName())
                .setStatusRemark(ticket.getStatusRemark())
                .setClosedAt(ticket.getClosedAt())
                .setCreatedAt(ticket.getCreatedAt())
                .setUpdatedAt(ticket.getUpdatedAt());
    }
}