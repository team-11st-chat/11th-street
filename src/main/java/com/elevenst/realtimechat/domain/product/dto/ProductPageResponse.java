package com.elevenst.realtimechat.domain.product.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record ProductPageResponse(
        List<ProductSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    public static ProductPageResponse from(Page<ProductSummaryResponse> products) {
        return new ProductPageResponse(
                products.getContent(),
                products.getNumber(),
                products.getSize(),
                products.getTotalElements(),
                products.getTotalPages()
        );
    }
}
