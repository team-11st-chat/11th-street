package com.elevenst.realtimechat.domain.promotion.service;

import java.math.BigDecimal;

public record TimeSalePurchaseSnapshot(
        Long productId,
        Long timeSaleId,
        String productName,
        BigDecimal originalPrice,
        BigDecimal salePrice
) {
}
