package com.elevenst.realtimechat.domain.product.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elevenst.realtimechat.domain.product.entity.Product;
import com.elevenst.realtimechat.domain.product.entity.SaleStatus;
import com.elevenst.realtimechat.domain.product.repository.ProductRepository;
import com.elevenst.realtimechat.global.config.CacheConfig;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(classes = {CacheConfig.class, ProductSearchService.class})
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
    void searchProductsWithCache_reusesCachedResponseForSameRequest() {
        when(productRepository.searchProducts(
                eq("airpods"), eq(11L), eq(SaleStatus.SUSPENDED), eq(SaleStatus.SOLD_OUT), any(PageRequest.class)
        )).thenReturn(new PageImpl<Product>(List.of(), PageRequest.of(0, 20), 0));

        productSearchService.searchProductsWithCache("airpods", 11L, 0, 20);
        productSearchService.searchProductsWithCache("airpods", 11L, 0, 20);

        verify(productRepository, times(1)).searchProducts(
                eq("airpods"), eq(11L), eq(SaleStatus.SUSPENDED), eq(SaleStatus.SOLD_OUT), any(PageRequest.class)
        );
    }

    @Test
    void searchProductsWithCache_distinguishesNullKeywordFromLiteralNullKeyword() {
        when(productRepository.searchProducts(
                any(), eq(11L), eq(SaleStatus.SUSPENDED), eq(SaleStatus.SOLD_OUT), any(PageRequest.class)
        )).thenReturn(new PageImpl<Product>(List.of(), PageRequest.of(0, 20), 0));

        productSearchService.searchProductsWithCache(null, 11L, 0, 20);
        productSearchService.searchProductsWithCache("null", 11L, 0, 20);

        verify(productRepository).searchProducts(
                eq(null), eq(11L), eq(SaleStatus.SUSPENDED), eq(SaleStatus.SOLD_OUT), any(PageRequest.class)
        );
        verify(productRepository).searchProducts(
                eq("null"), eq(11L), eq(SaleStatus.SUSPENDED), eq(SaleStatus.SOLD_OUT), any(PageRequest.class)
        );
    }
}
