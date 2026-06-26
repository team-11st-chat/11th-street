package com.elevenst.realtimechat.domain.product.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.entity.SaleStatus;
import com.elevenst.realtimechat.domain.product.repository.ProductRepository;
import com.elevenst.realtimechat.global.config.CacheConfig;
import com.github.benmanes.caffeine.cache.Policy;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {CacheConfig.class, ProductSearchService.class})
@Tag("integration")
class ProductSearchServiceCacheTest {

    @Autowired
    private ProductSearchService productSearchService;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        Cache cache = cacheManager.getCache(CacheConfig.PRODUCT_SEARCH_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void productSearchCache_usesConfiguredTtlAndMaximumSize() {
        Cache cache = cacheManager.getCache(CacheConfig.PRODUCT_SEARCH_CACHE);

        assertThat(cache).isNotNull();
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = nativeCaffeineCache(cache);
        Policy.FixedExpiration<Object, Object> expiration = nativeCache.policy().expireAfterWrite().orElseThrow();
        Policy.Eviction<Object, Object> eviction = nativeCache.policy().eviction().orElseThrow();

        assertThat(expiration.getExpiresAfter(TimeUnit.SECONDS))
                .isEqualTo(CacheConfig.PRODUCT_SEARCH_TTL.toSeconds());
        assertThat(eviction.getMaximum()).isEqualTo(CacheConfig.PRODUCT_SEARCH_MAXIMUM_SIZE);
    }

    @Test
    void searchProductsWithCache_storesResponseByProductSearchKeyPolicy() {
        when(productRepository.searchProducts(
                eq("airpods"), eq(11L), eq(SaleStatus.SUSPENDED), any(PageRequest.class)
        )).thenReturn(new PageImpl<Product>(List.of(), PageRequest.of(0, 20), 0));

        productSearchService.searchProductsWithCache("airpods", 11L, 0, 20);

        Cache cache = cacheManager.getCache(CacheConfig.PRODUCT_SEARCH_CACHE);
        assertThat(cache).isNotNull();
        assertThat(cache.get("product_search:airpods:11:0:20")).isNotNull();
    }

    @Test
    void searchProductsWithCache_reusesCachedResponseForSameRequest() {
        when(productRepository.searchProducts(
                eq("airpods"), eq(11L), eq(SaleStatus.SUSPENDED), any(PageRequest.class)
        )).thenReturn(new PageImpl<Product>(List.of(), PageRequest.of(0, 20), 0));

        productSearchService.searchProductsWithCache("airpods", 11L, 0, 20);
        productSearchService.searchProductsWithCache("airpods", 11L, 0, 20);

        verify(productRepository, times(1)).searchProducts(
                eq("airpods"), eq(11L), eq(SaleStatus.SUSPENDED), any(PageRequest.class)
        );
    }

    @Test
    void searchProducts_withoutCacheDoesNotReuseCachedResponse() {
        when(productRepository.searchProducts(
                eq("airpods"), eq(11L), eq(SaleStatus.SUSPENDED), any(PageRequest.class)
        )).thenReturn(new PageImpl<Product>(List.of(), PageRequest.of(0, 20), 0));

        productSearchService.searchProducts("airpods", 11L, 0, 20);
        productSearchService.searchProducts("airpods", 11L, 0, 20);

        verify(productRepository, times(2)).searchProducts(
                eq("airpods"), eq(11L), eq(SaleStatus.SUSPENDED), any(PageRequest.class)
        );
    }

    @Test
    void searchProductsWithCache_distinguishesNullKeywordFromLiteralNullKeyword() {
        when(productRepository.searchProducts(
                any(), eq(11L), eq(SaleStatus.SUSPENDED), any(PageRequest.class)
        )).thenReturn(new PageImpl<Product>(List.of(), PageRequest.of(0, 20), 0));

        productSearchService.searchProductsWithCache(null, 11L, 0, 20);
        productSearchService.searchProductsWithCache("null", 11L, 0, 20);

        verify(productRepository).searchProducts(
                eq(null), eq(11L), eq(SaleStatus.SUSPENDED), any(PageRequest.class)
        );
        verify(productRepository).searchProducts(
                eq("null"), eq(11L), eq(SaleStatus.SUSPENDED), any(PageRequest.class)
        );
    }

    @SuppressWarnings("unchecked")
    private com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCaffeineCache(Cache cache) {
        return (com.github.benmanes.caffeine.cache.Cache<Object, Object>) cache.getNativeCache();
    }
}
