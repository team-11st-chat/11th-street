package com.elevenst.realtimechat.domain.promotion.service;

import com.elevenst.realtimechat.domain.promotion.dto.IssuedCouponResponse;
import com.elevenst.realtimechat.domain.promotion.exception.CouponErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.CouponException;
import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.exception.CommonErrorCode;
import com.elevenst.realtimechat.global.support.IdempotencyManager;
import com.elevenst.realtimechat.global.support.LockManager;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 선착순 발급의 동시성 경계를 담당한다.
 * 분산 락 획득/해제 라이프사이클을 트랜잭션 바깥에 두어, 트랜잭션이 완전히 커밋된 뒤 락이 풀리도록 보장한다.
 * (락 해제가 커밋보다 먼저 일어나면 다음 대기 요청이 미커밋 상태로 수량·중복 검증을 우회할 수 있다.)
 */
@Component
@RequiredArgsConstructor
public class CouponIssueFacade {

    private static final long REQUEST_ID_TTL_SECONDS = 10;
    private static final long LOCK_WAIT_SECONDS = 3;
    private static final long LOCK_LEASE_SECONDS = 2;

    private final CouponIssueService couponIssueService;
    private final LockManager lockManager;
    private final IdempotencyManager idempotencyManager;

    public IssuedCouponResponse issueCoupon(Long memberId, Long couponPolicyId, String requestId) {
        // Request-ID 기반 멱등성 보호: 키 선점 이후 실패한 동일 Request-ID도 TTL 동안 중복 요청으로 처리한다.
        if (!idempotencyManager.checkAndSet(requestId, REQUEST_ID_TTL_SECONDS)) {
            throw new CouponException(CouponErrorCode.COUPON_003);
        }

        // 선착순 초과·중복 발급 방지를 위한 분산 락 (lock:coupon:{couponPolicyId}, Wait 3s / Lease 2s)
        String lockKey = "lock:coupon:" + couponPolicyId;
        boolean locked = lockManager.tryLock(lockKey, LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        if (!locked) {
            // 락 획득 실패/Redis 장애 시 Fail-Closed 거절 (503)
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE);
        }

        try {
            // @Transactional 서비스 호출이 반환되면 커밋이 끝난 상태이므로, 그 이후 finally 에서 락을 해제한다.
            return couponIssueService.issueCoupon(memberId, couponPolicyId);
        } finally {
            lockManager.unlock(lockKey);
        }
    }
}
