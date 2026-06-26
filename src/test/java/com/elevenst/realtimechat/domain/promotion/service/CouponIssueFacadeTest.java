package com.elevenst.realtimechat.domain.promotion.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elevenst.realtimechat.domain.promotion.dto.IssuedCouponResponse;
import com.elevenst.realtimechat.domain.promotion.entity.IssuedCouponStatus;
import com.elevenst.realtimechat.domain.promotion.exception.CouponErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.CouponException;
import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.exception.CommonErrorCode;
import com.elevenst.realtimechat.global.support.IdempotencyManager;
import com.elevenst.realtimechat.global.support.LockManager;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponIssueFacadeTest {

    @Mock
    private CouponIssueService couponIssueService;

    @Mock
    private LockManager lockManager;

    @Mock
    private IdempotencyManager idempotencyManager;

    private CouponIssueFacade couponIssueFacade;

    @BeforeEach
    void setUp() {
        couponIssueFacade = new CouponIssueFacade(couponIssueService, lockManager, idempotencyManager);
    }

    @Test
    @DisplayName("동일 Request-ID는 Lock 획득 전에 중복 발급 예외로 거절한다")
    void duplicatedRequestIdIsRejectedBeforeLock() {
        Long memberId = 1L;
        Long couponPolicyId = 10L;
        String requestId = "request-1";
        when(idempotencyManager.checkAndSet(requestId, 10)).thenReturn(false);

        assertThatThrownBy(() -> couponIssueFacade.issueCoupon(memberId, couponPolicyId, requestId))
                .isInstanceOf(CouponException.class)
                .extracting(exception -> ((CouponException) exception).getErrorCode())
                .isEqualTo(CouponErrorCode.COUPON_003);

        verify(lockManager, never()).tryLock("lock:coupon:" + couponPolicyId, 3, 2, TimeUnit.SECONDS);
        verify(couponIssueService, never()).issueCoupon(memberId, couponPolicyId);
    }

    @Test
    @DisplayName("쿠폰 Lock 획득에 실패하면 Fail-Closed로 발급 생성을 차단한다")
    void lockFailureIsRejectedFailClosed() {
        Long memberId = 1L;
        Long couponPolicyId = 10L;
        String requestId = "request-1";
        String lockKey = "lock:coupon:" + couponPolicyId;
        when(idempotencyManager.checkAndSet(requestId, 10)).thenReturn(true);
        when(lockManager.tryLock(lockKey, 3, 2, TimeUnit.SECONDS)).thenReturn(false);

        assertThatThrownBy(() -> couponIssueFacade.issueCoupon(memberId, couponPolicyId, requestId))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.SERVICE_UNAVAILABLE);

        verify(couponIssueService, never()).issueCoupon(memberId, couponPolicyId);
        verify(lockManager, never()).unlock(lockKey);
    }

    @Test
    @DisplayName("쿠폰 Lock 획득 후 발급 서비스를 호출하고 Lock을 해제한다")
    void issueIsDelegatedInsideCouponLock() {
        Long memberId = 1L;
        Long couponPolicyId = 10L;
        String requestId = "request-1";
        IssuedCouponResponse response = new IssuedCouponResponse(
                100L,
                couponPolicyId,
                memberId,
                IssuedCouponStatus.ISSUED,
                LocalDateTime.now()
        );
        String lockKey = "lock:coupon:" + couponPolicyId;
        when(idempotencyManager.checkAndSet(requestId, 10)).thenReturn(true);
        when(lockManager.tryLock(lockKey, 3, 2, TimeUnit.SECONDS)).thenReturn(true);
        when(couponIssueService.issueCoupon(memberId, couponPolicyId)).thenReturn(response);

        IssuedCouponResponse result = couponIssueFacade.issueCoupon(memberId, couponPolicyId, requestId);

        assertThat(result).isEqualTo(response);
        InOrder inOrder = inOrder(idempotencyManager, lockManager, couponIssueService);
        inOrder.verify(idempotencyManager).checkAndSet(requestId, 10);
        inOrder.verify(lockManager).tryLock(lockKey, 3, 2, TimeUnit.SECONDS);
        inOrder.verify(couponIssueService).issueCoupon(memberId, couponPolicyId);
        inOrder.verify(lockManager).unlock(lockKey);
    }

    @Test
    @DisplayName("발급 서비스에서 예외가 발생해도 획득한 Lock은 해제한다")
    void lockIsReleasedWhenIssueServiceThrowsException() {
        Long memberId = 1L;
        Long couponPolicyId = 10L;
        String requestId = "request-1";
        String lockKey = "lock:coupon:" + couponPolicyId;
        CouponException exception = new CouponException(CouponErrorCode.COUPON_002);
        when(idempotencyManager.checkAndSet(requestId, 10)).thenReturn(true);
        when(lockManager.tryLock(lockKey, 3, 2, TimeUnit.SECONDS)).thenReturn(true);
        when(couponIssueService.issueCoupon(memberId, couponPolicyId)).thenThrow(exception);

        assertThatThrownBy(() -> couponIssueFacade.issueCoupon(memberId, couponPolicyId, requestId))
                .isSameAs(exception);

        verify(lockManager).unlock(lockKey);
    }
}
