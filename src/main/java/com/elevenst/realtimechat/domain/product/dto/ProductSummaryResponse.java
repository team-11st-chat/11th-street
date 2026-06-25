package com.elevenst.realtimechat.domain.product.dto;

import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.entity.SaleStatus;
import java.math.BigDecimal;

public record ProductSummaryResponse(
        Long id,
        String name,
        BigDecimal price,
        SaleStatus saleStatus,
        Long categoryId
) {

    public static ProductSummaryResponse from(Product product) {
        return new ProductSummaryResponse(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getSaleStatus(),
                product.getCategory().getId()
        );
    }
}
