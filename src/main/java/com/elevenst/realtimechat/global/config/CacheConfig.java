package com.elevenst.realtimechat.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching(proxyTargetClass = true)
public class CacheConfig {

    public static final String PRODUCT_SEARCH_CACHE = "product_search";
    public static final String POPULAR_KEYWORDS_CACHE = "popular_keywords";
    public static final String POPULAR_KEYWORDS_KEY = "popular_keywords";

    public static final Duration PRODUCT_SEARCH_TTL = Duration.ofMinutes(10);
    public static final Duration POPULAR_KEYWORDS_TTL = Duration.ofMinutes(1);
    public static final long PRODUCT_SEARCH_MAXIMUM_SIZE = 10_000L;

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                new CaffeineCache(PRODUCT_SEARCH_CACHE, Caffeine.newBuilder()
                        .expireAfterWrite(PRODUCT_SEARCH_TTL)
                        .maximumSize(PRODUCT_SEARCH_MAXIMUM_SIZE)
                        .recordStats()
                        .build()),
                new CaffeineCache(POPULAR_KEYWORDS_CACHE, Caffeine.newBuilder()
                        .expireAfterWrite(POPULAR_KEYWORDS_TTL)
                        .recordStats()
                        .build())
        ));
        cacheManager.initializeCaches();
        return new TransactionAwareCacheManagerProxy(cacheManager);
    }
}
