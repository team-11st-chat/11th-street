package com.elevenst.realtimechat.domain.product.dto;

import com.elevenst.realtimechat.domain.product.entity.SaleStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductUpdateRequest(
        @Size(max = 100)
        String name,

        Long categoryId,

        @Positive
        BigDecimal price,

        @Min(0)
        Integer stockQuantity,

        SaleStatus saleStatus
) {
}
