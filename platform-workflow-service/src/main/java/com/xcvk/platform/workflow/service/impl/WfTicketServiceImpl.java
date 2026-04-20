package com.xcvk.platform.workflow.service.impl;

import com.xcvk.platform.workflow.model.entity.WfTicket;
import com.xcvk.platform.workflow.repository.mapper.WfTicketMapper;
import com.xcvk.platform.workflow.service.IWfTicketService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 工单主表 服务实现类
 * </p>
 *
 * @author Programmer
 * @since 2026-04-20
 */
@Service
public class WfTicketServiceImpl extends ServiceImpl<WfTicketMapper, WfTicket> implements IWfTicketService {

}
