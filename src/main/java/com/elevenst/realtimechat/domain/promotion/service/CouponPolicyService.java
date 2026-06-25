package com.elevenst.realtimechat.domain.promotion.service;

import com.elevenst.realtimechat.domain.member.entity.MemberRole;
import com.elevenst.realtimechat.domain.promotion.dto.CouponPolicyCreateRequest;
import com.elevenst.realtimechat.domain.promotion.dto.CouponPolicyResponse;
import com.elevenst.realtimechat.domain.promotion.entity.CouponPolicy;
import com.elevenst.realtimechat.domain.promotion.exception.CouponErrorCode;
import com.elevenst.realtimechat.domain.promotion.exception.CouponException;
import com.elevenst.realtimechat.domain.promotion.repository.CouponPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponPolicyService {

    private final CouponPolicyRepository couponPolicyRepository;

    @Transactional
    public CouponPolicyResponse createCouponPolicy(MemberRole role, CouponPolicyCreateRequest request) {
        validateSuperAdmin(role);

        CouponPolicy couponPolicy = new CouponPolicy(
                request.name(),
                request.discountType(),
                request.discountValue(),
                request.maxDiscountAmount(),
                request.issueStartsAt(),
                request.issueEndsAt(),
                request.totalQuantity()
        );
        couponPolicyRepository.save(couponPolicy);

        return CouponPolicyResponse.from(couponPolicy);
    }

    private void validateSuperAdmin(MemberRole role) {
        if (role != MemberRole.SUPER_ADMIN) {
            throw new CouponException(CouponErrorCode.UNAUTHORIZED_ADMIN);
        }
    }
}
