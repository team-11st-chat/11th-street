package com.elevenst.realtimechat.domain.promotion.service;

import com.elevenst.realtimechat.domain.promotion.dto.IssuedCouponResponse;
import com.elevenst.realtimechat.domain.promotion.entity.CouponPolicy;
import com.elevenst.realtimechat.domain.promotion.entity.IssuedCoupon;
import com.elevenst.realtimechat.domain.promotion.exception.CouponErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.CouponException;
import com.elevenst.realtimechat.domain.promotion.repository.CouponPolicyRepository;
import com.elevenst.realtimechat.domain.promotion.repository.IssuedCouponRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponIssueService {

    private final CouponPolicyRepository couponPolicyRepository;
    private final IssuedCouponRepository issuedCouponRepository;

    /**
     * 발급의 트랜잭션 경계만 담당한다. 분산 락/멱등성 보호는 {@link CouponIssueFacade} 가 트랜잭션 바깥에서 수행한다.
     */
    @Transactional
    public IssuedCouponResponse issueCoupon(Long memberId, Long couponPolicyId) {
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
    }
}
