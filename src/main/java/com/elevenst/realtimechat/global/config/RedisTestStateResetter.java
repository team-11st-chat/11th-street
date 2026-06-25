package com.elevenst.realtimechat.global.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class RedisTestStateResetter {

    private static final int SCAN_COUNT = 1_000;

    private final StringRedisTemplate redisTemplate;
    private final RedisKeyPrefixProperties redisKeyPrefixProperties;

    public RedisTestStateResetter(
            StringRedisTemplate redisTemplate,
            RedisKeyPrefixProperties redisKeyPrefixProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.redisKeyPrefixProperties = redisKeyPrefixProperties;
    }

    public void reset() {
        redisKeyPrefixProperties.validateSeparated();
        deleteByPattern(redisKeyPrefixProperties.cacheKey("*"));
        deleteByPattern(redisKeyPrefixProperties.lockKey("*"));
    }

    private void deleteByPattern(String pattern) {
        List<String> keys = scanKeys(pattern);
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private List<String> scanKeys(String pattern) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(SCAN_COUNT)
                .build();

        List<String> keys = new ArrayList<>();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(keys::add);
        }
        return keys;
    }
}
