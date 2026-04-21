package com.xcvk.platform.workflow.service;

import com.xcvk.platform.workflow.model.entity.TicketType;
import com.xcvk.platform.workflow.model.vo.TicketTypeOptionVO;

import java.util.List;

/**
 * 工单类型服务接口
 *
 * <p>当前阶段主要提供启用状态的工单类型选项查询。</p>
 *
 * @author Programmer
 * @version 1.0
 * @date 2026-04-20
 */
public interface TicketTypeService {

    /**
     * 查询启用状态的工单类型选项
     *
     * @return 工单类型选项列表
     */
    List<TicketTypeOptionVO> listEnabledOptions();

    TicketType getEnabledTicketType(String ticketTypeCode);
}