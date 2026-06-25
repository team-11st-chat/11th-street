package com.elevenst.realtimechat.domain.promotion.service;

import com.elevenst.realtimechat.domain.promotion.dto.IssuedCouponResponse;
import com.elevenst.realtimechat.domain.promotion.entity.CouponPolicy;
import com.elevenst.realtimechat.domain.promotion.entity.IssuedCoupon;
import com.elevenst.realtimechat.domain.promotion.exception.CouponErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.CouponException;
import com.elevenst.realtimechat.domain.promotion.repository.CouponPolicyRepository;
import com.elevenst.realtimechat.domain.promotion.repository.IssuedCouponRepository;
import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.exception.CommonErrorCode;
import com.elevenst.realtimechat.global.support.IdempotencyManager;
import com.elevenst.realtimechat.global.support.LockManager;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private static final long REQUEST_ID_TTL_SECONDS = 10;
    private static final long LOCK_WAIT_SECONDS = 3;
    private static final long LOCK_LEASE_SECONDS = 2;

    private final CouponPolicyRepository couponPolicyRepository;
    private final IssuedCouponRepository issuedCouponRepository;
    private final LockManager lockManager;
    private final IdempotencyManager idempotencyManager;

    @Transactional
    public IssuedCouponResponse issueCoupon(Long memberId, Long couponPolicyId, String requestId) {
        // Request-ID 기반 멱등성 보호: 동일 요청 재시도 시 중복 발급으로 처리한다.
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
            LocalDateTime now = LocalDateTime.now();

            CouponPolicy couponPolicy = couponPolicyRepository.findById(couponPolicyId)
                    .orElseThrow(() -> new CouponException(CouponErrorCode.COUPON_POLICY_NOT_FOUND));

            if (issuedCouponRepository.existsByCouponPolicyIdAndMemberId(couponPolicyId, memberId)) {
                throw new CouponException(CouponErrorCode.COUPON_003);
            }

            // 발급 가능 상태(기간) 검증 + 잔여 수량 차감 (COUPON_001 / COUPON_002)
            couponPolicy.issue(now);

            IssuedCoupon issuedCoupon = new IssuedCoupon(couponPolicyId, memberId, now);
            issuedCouponRepository.save(issuedCoupon);

            return IssuedCouponResponse.from(issuedCoupon);
        } finally {
            lockManager.unlock(lockKey);
        }
    }
}
