package com.elevenst.realtimechat.domain.product.service;

import static com.elevenst.realtimechat.global.config.CacheConfig.PRODUCT_SEARCH_CACHE;

import com.elevenst.realtimechat.domain.product.dto.ProductPageResponse;
import com.elevenst.realtimechat.domain.product.dto.ProductSummaryResponse;
import com.elevenst.realtimechat.domain.product.entity.SaleStatus;
import com.elevenst.realtimechat.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductSearchService {

    private static final String PRODUCT_SEARCH_CACHE_KEY =
            "'product_search:' + (#normalizedKeyword != null ? #normalizedKeyword : '~null~') + ':' + #categoryId + ':' + #page + ':' + #size";

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public ProductPageResponse searchProducts(String normalizedKeyword, Long categoryId, int page, int size) {
        return search(normalizedKeyword, categoryId, page, size);
    }

    @Cacheable(
            cacheNames = PRODUCT_SEARCH_CACHE,
            key = PRODUCT_SEARCH_CACHE_KEY
    )
    @Transactional(readOnly = true)
    public ProductPageResponse searchProductsWithCache(String normalizedKeyword, Long categoryId, int page, int size) {
        return search(normalizedKeyword, categoryId, page, size);
    }

    @Cacheable(
            cacheNames = PRODUCT_SEARCH_CACHE,
            cacheManager = "redisCacheManager",
            key = PRODUCT_SEARCH_CACHE_KEY
    )
    @Transactional(readOnly = true)
    public ProductPageResponse searchProductsWithRemoteCache(String normalizedKeyword, Long categoryId, int page, int size) {
        return search(normalizedKeyword, categoryId, page, size);
    }

    private ProductPageResponse search(String normalizedKeyword, Long categoryId, int page, int size) {
        return ProductPageResponse.from(productRepository
                .searchProducts(normalizedKeyword, categoryId, SaleStatus.SUSPENDED, PageRequest.of(page, size))
                .map(ProductSummaryResponse::from));
    }
}
