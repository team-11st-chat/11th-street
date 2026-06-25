package com.elevenst.realtimechat.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.redis.key-prefix")
public class RedisKeyPrefixProperties {

    private String cache = "cache:";
    private String lock = "lock:";

    public String getCache() {
        return cache;
    }

    public void setCache(String cache) {
        this.cache = cache;
    }

    public String getLock() {
        return lock;
    }

    public void setLock(String lock) {
        this.lock = lock;
    }

    public String cacheKey(String key) {
        return cache + key;
    }

    public String lockKey(String key) {
        return lock + key;
    }

    public void validateSeparated() {
        if (!StringUtils.hasText(cache) || !StringUtils.hasText(lock)) {
            throw new IllegalStateException("Redis key prefixes must not be blank.");
        }
        if (cache.equals(lock)) {
            throw new IllegalStateException("Redis cache and lock key prefixes must be different.");
        }
    }
}
