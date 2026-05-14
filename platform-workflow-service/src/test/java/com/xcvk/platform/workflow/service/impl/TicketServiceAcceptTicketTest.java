package com.xcvk.platform.workflow.service.impl;

import com.xcvk.platform.auth.starter.constant.PlatformRoleConstants;
import com.xcvk.platform.auth.starter.model.CurrentLoginIdentity;
import com.xcvk.platform.common.exception.BusinessException;
import com.xcvk.platform.id.generator.SnowflakeIdGenerator;
import com.xcvk.platform.workflow.assembler.TicketAssembler;
import com.xcvk.platform.workflow.constant.TicketStatusConstants;
import com.xcvk.platform.workflow.model.entity.Ticket;
import com.xcvk.platform.workflow.repository.mapper.TicketMapper;
import com.xcvk.platform.workflow.search.assembler.TicketSearchAssembler;
import com.xcvk.platform.workflow.search.model.index.TicketIndex;
import com.xcvk.platform.workflow.search.repository.TicketIndexRepository;
import com.xcvk.platform.workflow.service.TicketEventService;
import com.xcvk.platform.workflow.service.TicketTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TicketServiceAcceptTicketTest {

    private TicketMapper ticketMapper;
    private TicketIndexRepository ticketIndexRepository;
    private TicketSearchAssembler ticketSearchAssembler;
    private TicketServiceImpl ticketService;
    private TicketEventService ticketEventService;

    @BeforeEach
    void setUp() {
        ticketMapper = mock(TicketMapper.class);
        ticketEventService = mock(TicketEventService.class);

        TicketTypeService ticketTypeService = mock(TicketTypeService.class);
        SnowflakeIdGenerator idGenerator = mock(SnowflakeIdGenerator.class);
        TicketAssembler ticketAssembler = mock(TicketAssembler.class);

        ticketIndexRepository = mock(TicketIndexRepository.class);
        ticketSearchAssembler = mock(TicketSearchAssembler.class);

        ticketService = new TicketServiceImpl(
                ticketTypeService,
                idGenerator,
                ticketAssembler,
                ticketIndexRepository,
                ticketSearchAssembler,
                ticketEventService
        );

        /*
         * TicketServiceImpl 继承了 MyBatis-Plus 的 ServiceImpl。
         * ServiceImpl 里面有一个 protected baseMapper 字段。
         *
         * 正常项目运行时，这个 baseMapper 会由 Spring / MyBatis 自动注入。
         * 但我们这里是不启动 Spring 的单元测试，所以要手动把 mock 出来的 ticketMapper 塞进去。
         */
        ReflectionTestUtils.setField(ticketService, "baseMapper", ticketMapper);
    }

    @Test
    @DisplayName("SUPPORT 用户可以接取 PENDING 且未分配的工单")
    void shouldAcceptPendingUnassignedTicketWhenUserIsSupport() {
        Long ticketId = 1001L;
        CurrentLoginIdentity identity = supportIdentity();

        Ticket pendingTicket = pendingUnassignedTicket(ticketId);

        /*
         * acceptTicket 方法里会调用两次 getById：
         *
         * 第一次：接单前查询工单，做业务校验。
         * 第二次：接单成功后重新查询最新工单，同步到 ES。
         *
         * 所以这里 thenReturn 写两个 pendingTicket。
         */
        when(ticketMapper.selectById(ticketId)).thenReturn(pendingTicket, pendingTicket);

        /*
         * 模拟数据库条件更新成功。
         *
         * rows = 1 表示：
         * UPDATE wf_ticket ... WHERE id = ? AND assignee_id IS NULL AND status = 'PENDING'
         * 成功更新了 1 行。
         */
        when(ticketMapper.acceptTicket(
                eq(ticketId),
                eq(identity.userId()),
                eq(identity.realName()),
                eq(TicketStatusConstants.PENDING),
                eq(TicketStatusConstants.PROCESSING)
        )).thenReturn(1);

        when(ticketSearchAssembler.toIndex(pendingTicket))
                .thenReturn(new TicketIndex().setId(ticketId));

        ticketService.acceptTicket(identity, ticketId);

        verify(ticketMapper).acceptTicket(
                ticketId,
                identity.userId(),
                identity.realName(),
                TicketStatusConstants.PENDING,
                TicketStatusConstants.PROCESSING
        );

        verify(ticketSearchAssembler).toIndex(pendingTicket);
        verify(ticketIndexRepository).save(any(TicketIndex.class));
    }

    @Test
    @DisplayName("ADMIN 用户也可以接取 PENDING 且未分配的工单")
    void shouldAcceptPendingUnassignedTicketWhenUserIsAdmin() {
        Long ticketId = 1002L;
        CurrentLoginIdentity identity = adminIdentity();

        Ticket pendingTicket = pendingUnassignedTicket(ticketId);

        when(ticketMapper.selectById(ticketId)).thenReturn(pendingTicket, pendingTicket);
        when(ticketMapper.acceptTicket(
                eq(ticketId),
                eq(identity.userId()),
                eq(identity.realName()),
                eq(TicketStatusConstants.PENDING),
                eq(TicketStatusConstants.PROCESSING)
        )).thenReturn(1);

        when(ticketSearchAssembler.toIndex(pendingTicket))
                .thenReturn(new TicketIndex().setId(ticketId));

        ticketService.acceptTicket(identity, ticketId);

        verify(ticketMapper).acceptTicket(
                ticketId,
                identity.userId(),
                identity.realName(),
                TicketStatusConstants.PENDING,
                TicketStatusConstants.PROCESSING
        );

        verify(ticketIndexRepository).save(any(TicketIndex.class));
    }

    @Test
    @DisplayName("普通员工没有接单权限")
    void shouldRejectAcceptWhenUserIsEmployee() {
        Long ticketId = 1003L;
        CurrentLoginIdentity identity = employeeIdentity();

        assertThrows(
                BusinessException.class,
                () -> ticketService.acceptTicket(identity, ticketId)
        );

        /*
         * 权限校验失败后，不应该继续查数据库，更不应该执行接单更新。
         */
        verify(ticketMapper, never()).selectById(any());
        verify(ticketMapper, never()).acceptTicket(any(), any(), any(), any(), any());
        verifyNoInteractions(ticketIndexRepository);
    }

    @Test
    @DisplayName("工单不存在时不允许接单")
    void shouldRejectAcceptWhenTicketNotFound() {
        Long ticketId = 1004L;
        CurrentLoginIdentity identity = supportIdentity();

        when(ticketMapper.selectById(ticketId)).thenReturn(null);

        assertThrows(
                BusinessException.class,
                () -> ticketService.acceptTicket(identity, ticketId)
        );

        verify(ticketMapper).selectById(ticketId);
        verify(ticketMapper, never()).acceptTicket(any(), any(), any(), any(), any());
        verifyNoInteractions(ticketIndexRepository);
    }

    @Test
    @DisplayName("已分配处理人的工单不允许再次接单")
    void shouldRejectAcceptWhenTicketAlreadyAssigned() {
        Long ticketId = 1005L;
        CurrentLoginIdentity identity = supportIdentity();

        Ticket assignedTicket = pendingUnassignedTicket(ticketId)
                .setAssigneeId(999L)
                .setAssigneeName("其他处理人");

        when(ticketMapper.selectById(ticketId)).thenReturn(assignedTicket);

        assertThrows(
                BusinessException.class,
                () -> ticketService.acceptTicket(identity, ticketId)
        );

        verify(ticketMapper).selectById(ticketId);
        verify(ticketMapper, never()).acceptTicket(any(), any(), any(), any(), any());
        verifyNoInteractions(ticketIndexRepository);
    }

    @Test
    @DisplayName("非 PENDING 状态的工单不允许接单")
    void shouldRejectAcceptWhenTicketStatusIsNotPending() {
        Long ticketId = 1006L;
        CurrentLoginIdentity identity = supportIdentity();

        Ticket processingTicket = pendingUnassignedTicket(ticketId)
                .setStatus(TicketStatusConstants.PROCESSING);

        when(ticketMapper.selectById(ticketId)).thenReturn(processingTicket);

        assertThrows(
                BusinessException.class,
                () -> ticketService.acceptTicket(identity, ticketId)
        );

        verify(ticketMapper).selectById(ticketId);
        verify(ticketMapper, never()).acceptTicket(any(), any(), any(), any(), any());
        verifyNoInteractions(ticketIndexRepository);
    }

    @Test
    @DisplayName("数据库条件更新影响行数为 0 时，说明工单已被抢占或状态已变化")
    void shouldRejectAcceptWhenConditionalUpdateAffectedZeroRows() {
        Long ticketId = 1007L;
        CurrentLoginIdentity identity = supportIdentity();

        Ticket pendingTicket = pendingUnassignedTicket(ticketId);

        when(ticketMapper.selectById(ticketId)).thenReturn(pendingTicket);

        /*
         * rows = 0 表示条件更新失败。
         *
         * 典型场景：
         * A、B 两个人同时接同一张工单。
         * A 先更新成功，工单状态变为 PROCESSING。
         * B 后更新时，WHERE status = 'PENDING' 已经不满足，所以影响行数为 0。
         */
        when(ticketMapper.acceptTicket(
                eq(ticketId),
                eq(identity.userId()),
                eq(identity.realName()),
                eq(TicketStatusConstants.PENDING),
                eq(TicketStatusConstants.PROCESSING)
        )).thenReturn(0);

        assertThrows(
                BusinessException.class,
                () -> ticketService.acceptTicket(identity, ticketId)
        );

        verify(ticketMapper).acceptTicket(
                ticketId,
                identity.userId(),
                identity.realName(),
                TicketStatusConstants.PENDING,
                TicketStatusConstants.PROCESSING
        );

        /*
         * 接单失败时，不应该同步 ES。
         */
        verifyNoInteractions(ticketIndexRepository);
    }

    @Test
    @DisplayName("接单时应该把当前用户 ID 和真实姓名传给 Mapper")
    void shouldPassCurrentUserInfoToMapperWhenAcceptTicket() {
        Long ticketId = 1008L;
        CurrentLoginIdentity identity = new CurrentLoginIdentity(
                888L,
                "support_888",
                "客服小王",
                List.of(PlatformRoleConstants.SUPPORT)
        );

        Ticket pendingTicket = pendingUnassignedTicket(ticketId);

        when(ticketMapper.selectById(ticketId)).thenReturn(pendingTicket, pendingTicket);
        when(ticketMapper.acceptTicket(any(), any(), any(), any(), any())).thenReturn(1);
        when(ticketSearchAssembler.toIndex(pendingTicket))
                .thenReturn(new TicketIndex().setId(ticketId));

        ticketService.acceptTicket(identity, ticketId);

        ArgumentCaptor<Long> assigneeIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> assigneeNameCaptor = ArgumentCaptor.forClass(String.class);

        verify(ticketMapper).acceptTicket(
                eq(ticketId),
                assigneeIdCaptor.capture(),
                assigneeNameCaptor.capture(),
                eq(TicketStatusConstants.PENDING),
                eq(TicketStatusConstants.PROCESSING)
        );

        assertEquals(888L, assigneeIdCaptor.getValue());
        assertEquals("客服小王", assigneeNameCaptor.getValue());
    }

    private CurrentLoginIdentity supportIdentity() {
        return new CurrentLoginIdentity(
                2001L,
                "support_user",
                "客服人员",
                List.of(PlatformRoleConstants.SUPPORT)
        );
    }

    private CurrentLoginIdentity adminIdentity() {
        return new CurrentLoginIdentity(
                1L,
                "admin",
                "管理员",
                List.of(PlatformRoleConstants.ADMIN)
        );
    }

    private CurrentLoginIdentity employeeIdentity() {
        return new CurrentLoginIdentity(
                3001L,
                "employee_user",
                "普通员工",
                List.of(PlatformRoleConstants.EMPLOYEE)
        );
    }

    private Ticket pendingUnassignedTicket(Long ticketId) {
        return new Ticket()
                .setId(ticketId)
                .setTicketNo("TK202605130001")
                .setTitle("测试工单")
                .setContent("这是一个测试工单")
                .setStatus(TicketStatusConstants.PENDING)
                .setCreatorId(3001L)
                .setCreatorName("普通员工")
                .setAssigneeId(null)
                .setAssigneeName(null);
    }
}