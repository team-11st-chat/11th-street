package com.elevenst.realtimechat.domain.promotion.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TimeSaleCreateRequest(
        @NotNull Long productId,
        @NotNull @Min(100) BigDecimal salePrice,
        @NotNull LocalDateTime startedAt,
        @NotNull LocalDateTime endedAt,
        @NotNull @Min(1) Integer initialQuantity
) {
}
