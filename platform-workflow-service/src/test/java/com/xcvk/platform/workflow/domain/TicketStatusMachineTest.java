package com.xcvk.platform.workflow.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.xcvk.platform.workflow.constant.TicketStatusConstants.PENDING;
import static com.xcvk.platform.workflow.constant.TicketStatusConstants.PROCESSING;
import static com.xcvk.platform.workflow.constant.TicketStatusConstants.REJECTED;
import static com.xcvk.platform.workflow.constant.TicketStatusConstants.RESOLVED;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketStatusMachineTest {

    @Test
    @DisplayName("PENDING 状态允许流转到 PROCESSING")
    void shouldAllowPendingToProcessing() {
        boolean result = TicketStatusMachine.canTransfer(PENDING, PROCESSING);

        assertTrue(result);
    }

    @Test
    @DisplayName("PROCESSING 状态允许流转到 RESOLVED 或 REJECTED")
    void shouldAllowProcessingToFinalStatus() {
        boolean resolvedResult = TicketStatusMachine.canTransfer(PROCESSING, RESOLVED);
        boolean rejectedResult = TicketStatusMachine.canTransfer(PROCESSING, REJECTED);

        assertTrue(resolvedResult);
        assertTrue(rejectedResult);
    }

    @Test
    @DisplayName("PENDING 状态不允许直接流转到 RESOLVED 或 REJECTED")
    void shouldNotAllowPendingToFinalStatus() {
        boolean resolvedResult = TicketStatusMachine.canTransfer(PENDING, RESOLVED);
        boolean rejectedResult = TicketStatusMachine.canTransfer(PENDING, REJECTED);

        assertFalse(resolvedResult);
        assertFalse(rejectedResult);
    }

    @Test
    @DisplayName("RESOLVED 和 REJECTED 是终态，不允许继续流转")
    void shouldNotAllowTransferFromFinalStatus() {
        boolean resolvedToProcessing = TicketStatusMachine.canTransfer(RESOLVED, PROCESSING);
        boolean rejectedToProcessing = TicketStatusMachine.canTransfer(REJECTED, PROCESSING);

        assertFalse(resolvedToProcessing);
        assertFalse(rejectedToProcessing);
    }

    @Test
    @DisplayName("非法状态或空状态不允许流转")
    void shouldNotAllowIllegalStatusTransfer() {
        assertFalse(TicketStatusMachine.canTransfer(null, PROCESSING));
        assertFalse(TicketStatusMachine.canTransfer(PENDING, null));
        assertFalse(TicketStatusMachine.canTransfer("", PROCESSING));
        assertFalse(TicketStatusMachine.canTransfer("UNKNOWN", PROCESSING));
        assertFalse(TicketStatusMachine.canTransfer(PENDING, "UNKNOWN"));
    }

    @Test
    @DisplayName("状态前后有空格时，状态机可以自动 trim 后再判断")
    void shouldTrimStatusBeforeTransfer() {
        boolean result = TicketStatusMachine.canTransfer("  PENDING  ", "  PROCESSING  ");

        assertTrue(result);
    }

    @Test
    @DisplayName("只允许处理人把工单更新为 RESOLVED 或 REJECTED")
    void shouldOnlyAllowResolvedOrRejectedAsProcessResultStatus() {
        assertTrue(TicketStatusMachine.isAllowedProcessResultStatus(RESOLVED));
        assertTrue(TicketStatusMachine.isAllowedProcessResultStatus(REJECTED));

        assertFalse(TicketStatusMachine.isAllowedProcessResultStatus(PENDING));
        assertFalse(TicketStatusMachine.isAllowedProcessResultStatus(PROCESSING));
        assertFalse(TicketStatusMachine.isAllowedProcessResultStatus(null));
        assertFalse(TicketStatusMachine.isAllowedProcessResultStatus("UNKNOWN"));
    }

    @Test
    @DisplayName("可以判断一个状态是否是系统已知状态")
    void shouldCheckKnownStatus() {
        assertTrue(TicketStatusMachine.isKnownStatus(PENDING));
        assertTrue(TicketStatusMachine.isKnownStatus(PROCESSING));
        assertTrue(TicketStatusMachine.isKnownStatus(RESOLVED));
        assertTrue(TicketStatusMachine.isKnownStatus(REJECTED));

        assertFalse(TicketStatusMachine.isKnownStatus(null));
        assertFalse(TicketStatusMachine.isKnownStatus(""));
        assertFalse(TicketStatusMachine.isKnownStatus("UNKNOWN"));
    }
}