package com.elevenst.realtimechat.domain.product.service;

import static com.elevenst.realtimechat.global.config.CacheConfig.PRODUCT_SEARCH_CACHE;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductSearchCacheEvictor {

    private final CacheManager cacheManager;
    private final ObjectProvider<CacheManager> redisCacheManagerProvider;
    private final ProductSearchCacheProperties properties;

    public ProductSearchCacheEvictor(
            CacheManager cacheManager,
            @Qualifier("redisCacheManager") ObjectProvider<CacheManager> redisCacheManagerProvider,
            ProductSearchCacheProperties properties
    ) {
        this.cacheManager = cacheManager;
        this.redisCacheManagerProvider = redisCacheManagerProvider;
        this.properties = properties;
    }

    public void evictAll() {
        // 1. Local Cache 무효화는 항상 수행
        Cache localCache = cacheManager.getCache(PRODUCT_SEARCH_CACHE);
        if (localCache != null) {
            localCache.clear();
        }

        // 2. Remote Cache 모드인 경우에만 Redis 캐시 무효화 수행
        if (properties.mode() == ProductSearchCacheProperties.Mode.REMOTE) {
            redisCacheManagerProvider.ifAvailable(redisCacheManager -> {
                try {
                    Cache remoteCache = redisCacheManager.getCache(PRODUCT_SEARCH_CACHE);
                    if (remoteCache != null) {
                        remoteCache.clear();
                    }
                } catch (Exception e) {
                    log.warn("Failed to clear remote redis cache: {}", e.getMessage(), e);
                }
            });
        }
    }
}
