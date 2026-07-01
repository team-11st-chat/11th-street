package com.elevenst.realtimechat.domain.promotion.dto;

import com.elevenst.realtimechat.domain.promotion.entity.CouponPolicy;
import com.elevenst.realtimechat.domain.promotion.entity.CouponPolicyStatus;
import com.elevenst.realtimechat.domain.promotion.entity.DiscountType;

public record CouponPolicyResponse(
        Long id,
        String name,
        DiscountType discountType,
        Long discountValue,
        Long maxDiscountAmount,
        int remainingQuantity,
        CouponPolicyStatus status
) {
    public static CouponPolicyResponse from(CouponPolicy couponPolicy) {
        return new CouponPolicyResponse(
                couponPolicy.getId(),
                couponPolicy.getName(),
                couponPolicy.getDiscountType(),
                couponPolicy.getDiscountValue(),
                couponPolicy.getMaxDiscountAmount(),
                couponPolicy.getRemainingQuantity(),
                couponPolicy.getStatus()
        );
    }
}
