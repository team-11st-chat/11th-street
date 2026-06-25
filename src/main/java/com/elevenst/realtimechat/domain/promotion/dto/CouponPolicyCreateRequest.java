package com.elevenst.realtimechat.domain.promotion.dto;

import com.elevenst.realtimechat.domain.promotion.entity.DiscountType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record CouponPolicyCreateRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotNull
        DiscountType discountType,

        @NotNull
        @Positive
        Long discountValue,

        @Positive
        Long maxDiscountAmount,

        @NotNull
        LocalDateTime issueStartsAt,

        @NotNull
        LocalDateTime issueEndsAt,

        @NotNull
        @Min(1)
        Integer totalQuantity
) {
}
