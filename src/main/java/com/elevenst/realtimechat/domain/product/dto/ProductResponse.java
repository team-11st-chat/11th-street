package com.elevenst.realtimechat.domain.product.dto;

import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.entity.SaleStatus;
import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        Long sellerId,
        Long categoryId,
        String name,
        BigDecimal price,
        int stockQuantity,
        SaleStatus saleStatus
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSellerId(),
                product.getCategory().getId(),
                product.getName(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getSaleStatus()
        );
    }
}
