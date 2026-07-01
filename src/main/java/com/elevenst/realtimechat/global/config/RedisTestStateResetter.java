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
    private static final int DELETE_CHUNK_SIZE = 500;

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
        deleteByPattern(redisKeyPrefixProperties.cacheKey("*"));
        deleteByPattern(redisKeyPrefixProperties.lockKey("*"));
    }

    private void deleteByPattern(String pattern) {
        ScanOptions options = ScanOptions.scanOptions()
                .match(pattern)
                .count(SCAN_COUNT)
                .build();

        List<String> keys = new ArrayList<>(DELETE_CHUNK_SIZE);
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            cursor.forEachRemaining(key -> {
                keys.add(key);
                if (keys.size() >= DELETE_CHUNK_SIZE) {
                    redisTemplate.delete(keys);
                    keys.clear();
                }
            });
        }

        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
