package com.elevenst.realtimechat.global.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
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
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        cacheManager.setCaches(List.of(
                new CaffeineCache(PRODUCT_SEARCH_CACHE, Caffeine.newBuilder()
                        .expireAfterWrite(PRODUCT_SEARCH_TTL)
                        .maximumSize(PRODUCT_SEARCH_MAXIMUM_SIZE)
                        .recordStats()
                        .build())
        ));
        return cacheManager;
    }
}
