package com.xcvk.platform.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcvk.platform.common.enums.CommonStatusEnum;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.common.util.BizAssert;
import com.xcvk.platform.workflow.constant.TicketErrorMessages;
import com.xcvk.platform.workflow.model.entity.TicketType;
import com.xcvk.platform.workflow.model.vo.TicketTypeOptionVO;
import com.xcvk.platform.workflow.repository.mapper.TicketTypeMapper;
import com.xcvk.platform.workflow.service.TicketTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工单类型服务实现类
 *
 * <p>当前阶段主要提供启用工单类型查询能力，
 * 供工单创建和类型选项展示复用。</p>
 *
 * @author Programmer
 * @since 2026-04-20
 */
@Service
public class TicketTypeServiceImpl extends ServiceImpl<TicketTypeMapper, TicketType> implements TicketTypeService {

    @Override
    public List<TicketTypeOptionVO> listEnabledOptions() {
        return List.of();
    }

    /**
     * 根据工单类型编码查询启用状态的工单类型。
     *
     * <p>如果工单类型不存在，或该类型当前不是启用状态，
     * 则视为不可用于工单创建。</p>
     *
     * @param ticketTypeCode 工单类型编码
     * @return 工单类型对象
     */
    @Override
    public TicketType getEnabledTicketType(String ticketTypeCode) {
        LambdaQueryWrapper<TicketType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TicketType::getTypeCode, ticketTypeCode)
                .eq(TicketType::getStatus, CommonStatusEnum.ENABLED.getCode());

        TicketType ticketType = getOne(queryWrapper);

        BizAssert.notNull(
                ticketType,
                ErrorCode.BIZ_ERROR,
                TicketErrorMessages.TICKET_TYPE_NOT_FOUND_OR_DISABLED
        );
        return ticketType;
    }
}