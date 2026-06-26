package com.elevenst.realtimechat.domain.product.service;

import static com.elevenst.realtimechat.global.config.CacheConfig.PRODUCT_SEARCH_CACHE;

import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class ProductSearchCacheEvictor {

    private final CacheManager cacheManager;
    private final ObjectProvider<CacheManager> redisCacheManagerProvider;

    public ProductSearchCacheEvictor(
            CacheManager cacheManager,
            @Qualifier("redisCacheManager") ObjectProvider<CacheManager> redisCacheManagerProvider
    ) {
        this.cacheManager = cacheManager;
        this.redisCacheManagerProvider = redisCacheManagerProvider;
    }

    public void evictAll() {
        Set<CacheManager> cacheManagers = new LinkedHashSet<>();
        cacheManagers.add(cacheManager);
        redisCacheManagerProvider.ifAvailable(cacheManagers::add);

        for (CacheManager manager : cacheManagers) {
            Cache cache = manager.getCache(PRODUCT_SEARCH_CACHE);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
