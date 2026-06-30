package com.elevenst.realtimechat.domain.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elevenst.realtimechat.domain.order.dto.TimeSaleOrderRequest;
import com.elevenst.realtimechat.domain.order.dto.TimeSaleOrderResponse;
import com.elevenst.realtimechat.domain.order.entity.TimeSaleOrderStatus;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.TimeSaleException;
import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.exception.CommonErrorCode;
import com.elevenst.realtimechat.global.support.IdempotencyManager;
import com.elevenst.realtimechat.global.support.LockManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimeSaleOrderFacadeTest {

    @Mock
    private TimeSaleOrderService timeSaleOrderService;

    @Mock
    private LockManager lockManager;

    @Mock
    private IdempotencyManager idempotencyManager;

    private TimeSaleOrderFacade timeSaleOrderFacade;

    @BeforeEach
    void setUp() {
        timeSaleOrderFacade = new TimeSaleOrderFacade(timeSaleOrderService, lockManager, idempotencyManager, 10L);
    }

    @Test
    @DisplayName("Lock 획득 후 Request-ID가 중복이면 타임세일 중복 주문 예외를 반환하고 Lock을 해제한다")
    void duplicatedRequestIdIsRejectedInsideLock() {
        Long memberId = 1L;
        Long timeSaleId = 10L;
        String requestId = "request-1";
        TimeSaleOrderRequest request = new TimeSaleOrderRequest(1);
        String lockKey = "lock:timesale:" + timeSaleId;
        when(lockManager.tryLock(lockKey)).thenReturn(true);
        when(idempotencyManager.checkAndSet(requestId, 10)).thenReturn(false);

        assertThatThrownBy(() -> timeSaleOrderFacade.orderTimeSale(memberId, timeSaleId, requestId, request))
                .isInstanceOf(TimeSaleException.class)
                .extracting(exception -> ((TimeSaleException) exception).getErrorCode())
                .isEqualTo(TimeSaleErrorCode.TIME_SALE_003);

        verify(timeSaleOrderService, never()).orderTimeSale(memberId, timeSaleId, requestId, request);
        verify(lockManager).unlock(lockKey);
    }

    @Test
    @DisplayName("타임세일 Lock 획득에 실패하면 멱등성 키를 선점하지 않고 Fail-Closed로 주문 생성을 차단한다")
    void lockFailureIsRejectedFailClosed() {
        Long memberId = 1L;
        Long timeSaleId = 10L;
        String requestId = "request-1";
        TimeSaleOrderRequest request = new TimeSaleOrderRequest(1);
        String lockKey = "lock:timesale:" + timeSaleId;
        when(lockManager.tryLock(lockKey)).thenReturn(false);

        assertThatThrownBy(() -> timeSaleOrderFacade.orderTimeSale(memberId, timeSaleId, requestId, request))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.SERVICE_UNAVAILABLE);

        verify(idempotencyManager, never()).checkAndSet(requestId, 10);
        verify(timeSaleOrderService, never()).orderTimeSale(memberId, timeSaleId, requestId, request);
        verify(lockManager, never()).unlock(lockKey);
    }

    @Test
    @DisplayName("타임세일 Lock 획득 후 주문 서비스를 호출하고 Lock을 해제한다")
    void orderIsDelegatedInsideTimeSaleLock() {
        Long memberId = 1L;
        Long timeSaleId = 10L;
        String requestId = "request-1";
        TimeSaleOrderRequest request = new TimeSaleOrderRequest(1);
        TimeSaleOrderResponse response = new TimeSaleOrderResponse(
                100L,
                timeSaleId,
                200L,
                1,
                new BigDecimal("9000"),
                TimeSaleOrderStatus.COMPLETED,
                LocalDateTime.now()
        );
        String lockKey = "lock:timesale:" + timeSaleId;
        when(lockManager.tryLock(lockKey)).thenReturn(true);
        when(idempotencyManager.checkAndSet(requestId, 10)).thenReturn(true);
        when(timeSaleOrderService.orderTimeSale(memberId, timeSaleId, requestId, request)).thenReturn(response);

        TimeSaleOrderResponse result = timeSaleOrderFacade.orderTimeSale(memberId, timeSaleId, requestId, request);

        assertThat(result).isEqualTo(response);
        InOrder inOrder = inOrder(idempotencyManager, lockManager, timeSaleOrderService);
        inOrder.verify(lockManager).tryLock(lockKey);
        inOrder.verify(idempotencyManager).checkAndSet(requestId, 10);
        inOrder.verify(timeSaleOrderService).orderTimeSale(memberId, timeSaleId, requestId, request);
        inOrder.verify(lockManager).unlock(lockKey);
    }

    @Test
    @DisplayName("주문 서비스에서 예외가 발생해도 획득한 Lock은 해제한다")
    void lockIsReleasedWhenOrderServiceThrowsException() {
        Long memberId = 1L;
        Long timeSaleId = 10L;
        String requestId = "request-1";
        TimeSaleOrderRequest request = new TimeSaleOrderRequest(1);
        String lockKey = "lock:timesale:" + timeSaleId;
        TimeSaleException exception = new TimeSaleException(TimeSaleErrorCode.TIME_SALE_002);
        when(idempotencyManager.checkAndSet(requestId, 10)).thenReturn(true);
        when(lockManager.tryLock(lockKey)).thenReturn(true);
        when(timeSaleOrderService.orderTimeSale(memberId, timeSaleId, requestId, request)).thenThrow(exception);

        assertThatThrownBy(() -> timeSaleOrderFacade.orderTimeSale(memberId, timeSaleId, requestId, request))
                .isSameAs(exception);

        verify(lockManager).unlock(lockKey);
    }
}
