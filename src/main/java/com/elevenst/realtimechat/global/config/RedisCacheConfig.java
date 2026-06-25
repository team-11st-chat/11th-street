package com.elevenst.realtimechat.global.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableConfigurationProperties(RedisKeyPrefixProperties.class)
public class RedisCacheConfig {

    public static final Duration DEFAULT_REDIS_CACHE_TTL = Duration.ofMinutes(10);

    @Bean(name = "redisCacheManager")
    public CacheManager redisCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            RedisKeyPrefixProperties redisKeyPrefixProperties
    ) {
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_REDIS_CACHE_TTL)
                .computePrefixWith(cacheName -> redisKeyPrefixProperties.cacheKey(cacheName + "::"))
                .disableCachingNullValues()
                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(SerializationPair.fromSerializer(
                        GenericJacksonJsonRedisSerializer.builder().build()));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfiguration)
                .transactionAware()
                .build();
    }
}
