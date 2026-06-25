package com.elevenst.realtimechat.domain.promotion.repository;

import com.elevenst.realtimechat.domain.promotion.entity.CouponPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponPolicyRepository extends JpaRepository<CouponPolicy, Long> {
}
