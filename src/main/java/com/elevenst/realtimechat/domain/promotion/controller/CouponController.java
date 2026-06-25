package com.elevenst.realtimechat.domain.promotion.controller;

import com.elevenst.realtimechat.domain.promotion.dto.CouponPolicyCreateRequest;
import com.elevenst.realtimechat.domain.promotion.dto.CouponPolicyResponse;
import com.elevenst.realtimechat.domain.promotion.dto.IssuedCouponResponse;
import com.elevenst.realtimechat.domain.promotion.service.CouponIssueFacade;
import com.elevenst.realtimechat.domain.promotion.service.CouponPolicyService;
import com.elevenst.realtimechat.global.response.ApiResponse;
import com.elevenst.realtimechat.global.security.AuthenticatedMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/coupons")
public class CouponController {

    private final CouponPolicyService couponPolicyService;
    private final CouponIssueFacade couponIssueFacade;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CouponPolicyResponse> createCouponPolicy(
            @AuthenticationPrincipal AuthenticatedMember member,
            @Valid @RequestBody CouponPolicyCreateRequest request
    ) {
        return ApiResponse.success(
                "쿠폰 정책이 등록되었습니다.",
                couponPolicyService.createCouponPolicy(member.role(), request)
        );
    }

    @PostMapping("/{couponPolicyId}/issue")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IssuedCouponResponse> issueCoupon(
            @PathVariable Long couponPolicyId,
            @RequestHeader(value = "Request-ID", required = true) String requestId,
            @AuthenticationPrincipal AuthenticatedMember member
    ) {
        return ApiResponse.success(
                "쿠폰이 발급되었습니다.",
                couponIssueFacade.issueCoupon(member.memberId(), couponPolicyId, requestId)
        );
    }

    // TODO(#25): '고객별 쿠폰 발급 결과 조회' API 는 API-Spec 섹션 8 에 경로/응답이 정의되지 않아 보류한다.
    //            스펙 확정 후 GET 엔드포인트(예: /api/v1/coupons/issued)로 구현 예정.
}
