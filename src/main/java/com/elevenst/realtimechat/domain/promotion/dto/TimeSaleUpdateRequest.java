package com.elevenst.realtimechat.domain.promotion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TimeSaleUpdateRequest(
        BigDecimal salePrice,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer initialQuantity
) {
}
