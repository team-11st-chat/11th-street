package com.elevenst.realtimechat.domain.promotion.repository;

import com.elevenst.realtimechat.domain.promotion.entity.IssuedCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssuedCouponRepository extends JpaRepository<IssuedCoupon, Long> {
    boolean existsByCouponPolicyIdAndMemberId(Long couponPolicyId, Long memberId);
}
