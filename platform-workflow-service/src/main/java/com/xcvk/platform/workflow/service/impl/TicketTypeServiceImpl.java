package com.xcvk.platform.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xcvk.platform.common.enums.CommonStatusEnum;
import com.xcvk.platform.common.exception.BusinessException;
import com.xcvk.platform.common.exception.ErrorCode;
import com.xcvk.platform.workflow.model.entity.TicketType;
import com.xcvk.platform.workflow.model.vo.TicketTypeOptionVO;
import com.xcvk.platform.workflow.repository.mapper.TicketTypeMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcvk.platform.workflow.service.TicketTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * 工单类型表 服务实现类
 * </p>
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
     * 根据工单类型编码获取工单类型：
     * 如果出现工单类型不存在或状态异常，则抛出异常。
     *
     *
     * @param ticketTypeCode 工单类型编码
     * @return 工单类型对象
     * */
    @Override
    public TicketType getEnabledTicketType(String ticketTypeCode) {
        LambdaQueryWrapper<TicketType> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TicketType::getTypeCode, ticketTypeCode)
                .eq(TicketType::getStatus, CommonStatusEnum.ENABLED.getCode());

        TicketType ticketType = getOne(queryWrapper);

        if (ticketType == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "工单类型不存在");
        }
        return ticketType;
    }
}
