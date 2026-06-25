package com.elevenst.realtimechat.domain.product.service;

public record ProductSearchCacheKey(
        String normalizedKeyword,
        Long categoryId,
        int page,
        int size
) {

    public static ProductSearchCacheKey of(String normalizedKeyword, Long categoryId, int page, int size) {
        return new ProductSearchCacheKey(normalizedKeyword, categoryId, page, size);
    }
}
