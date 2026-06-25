package com.elevenst.realtimechat.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.cache.transaction.TransactionAwareCacheManagerProxy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRODUCT_SEARCH_CACHE = "product_search";

    private static final Duration PRODUCT_SEARCH_TTL = Duration.ofMinutes(10);
    private static final long PRODUCT_SEARCH_MAXIMUM_SIZE = 10_000L;

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(PRODUCT_SEARCH_CACHE);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(PRODUCT_SEARCH_TTL)
                .maximumSize(PRODUCT_SEARCH_MAXIMUM_SIZE)
                .recordStats());
        return new TransactionAwareCacheManagerProxy(cacheManager);
    }
}
