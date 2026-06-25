package com.elevenst.realtimechat.domain.promotion.dto;

import com.elevenst.realtimechat.domain.promotion.entity.TimeSale;
import com.elevenst.realtimechat.domain.promotion.entity.TimeSaleStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TimeSaleResponse(
        Long id,
        Long productId,
        BigDecimal originalPrice,
        BigDecimal salePrice,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        TimeSaleStatus status,
        int remainingQuantity
) {
    public static TimeSaleResponse of(TimeSale timeSale, int remainingQuantity) {
        return new TimeSaleResponse(
                timeSale.getId(),
                timeSale.getProduct().getId(),
                timeSale.getOriginalPrice(),
                timeSale.getSalePrice(),
                timeSale.getStartedAt(),
                timeSale.getEndedAt(),
                timeSale.getStatus(),
                remainingQuantity
        );
    }
}
