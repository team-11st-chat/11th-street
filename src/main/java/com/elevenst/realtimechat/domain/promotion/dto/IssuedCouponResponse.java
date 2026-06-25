package com.elevenst.realtimechat.domain.promotion.dto;

import com.elevenst.realtimechat.domain.promotion.entity.IssuedCoupon;
import com.elevenst.realtimechat.domain.promotion.entity.IssuedCouponStatus;
import java.time.LocalDateTime;

public record IssuedCouponResponse(
        Long id,
        Long couponPolicyId,
        Long memberId,
        IssuedCouponStatus status,
        LocalDateTime issuedAt
) {
    public static IssuedCouponResponse from(IssuedCoupon issuedCoupon) {
        return new IssuedCouponResponse(
                issuedCoupon.getId(),
                issuedCoupon.getCouponPolicyId(),
                issuedCoupon.getMemberId(),
                issuedCoupon.getStatus(),
                issuedCoupon.getIssuedAt()
        );
    }
}
