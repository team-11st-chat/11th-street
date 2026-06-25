package com.elevenst.realtimechat.global.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.util.StringUtils;

@Validated
@ConfigurationProperties(prefix = "app.redis.key-prefix")
public class RedisKeyPrefixProperties {

    // Bump this version when cached DTO serialization shape changes and old Redis cache entries must be ignored.
    @NotBlank
    private String cache = "cache:v1:";

    @NotBlank
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

    @PostConstruct
    public void validateSeparated() {
        if (!StringUtils.hasText(cache) || !StringUtils.hasText(lock)) {
            throw new IllegalStateException("Redis key prefixes must not be blank.");
        }
        if (cache.equals(lock)) {
            throw new IllegalStateException("Redis cache and lock key prefixes must be different.");
        }
    }
}
