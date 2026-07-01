package com.elevenst.realtimechat.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.elevenst.realtimechat.domain.product.service.ProductSearchCacheProperties;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@EnableCaching(proxyTargetClass = true)
@EnableConfigurationProperties(ProductSearchCacheProperties.class)
public class CacheConfig {

    public static final String PRODUCT_SEARCH_CACHE = "product_search";
    public static final String POPULAR_KEYWORDS_CACHE = "popular_keywords";
    public static final String POPULAR_KEYWORDS_KEY = "top";

    private final Duration productSearchTtl;
    private final Duration popularKeywordsTtl;
    private final long productSearchMaximumSize;

    public CacheConfig(
            @Value("${app.cache.caffeine.product-search-ttl:10m}") Duration productSearchTtl,
            @Value("${app.cache.caffeine.popular-keywords-ttl:1m}") Duration popularKeywordsTtl,
            @Value("${app.cache.caffeine.product-search-maximum-size:10000}") long productSearchMaximumSize
    ) {
        this.productSearchTtl = productSearchTtl;
        this.popularKeywordsTtl = popularKeywordsTtl;
        this.productSearchMaximumSize = productSearchMaximumSize;
    }

    @Bean
    @Primary
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
            new CaffeineCache(PRODUCT_SEARCH_CACHE, Caffeine.newBuilder()
                .expireAfterWrite(productSearchTtl)
                .maximumSize(productSearchMaximumSize)
                .recordStats()
                .build()),
            new CaffeineCache(POPULAR_KEYWORDS_CACHE, Caffeine.newBuilder()
                .expireAfterWrite(popularKeywordsTtl)
                .recordStats()
                .build())
        ));
        cacheManager.initializeCaches();
        return new TransactionAwareCacheManagerProxy(cacheManager);
    }
}
