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
import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

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
                        GenericJacksonJsonRedisSerializer.builder()
                                .enableDefaultTyping(new RedisCachePolymorphicTypeValidator())
                                .build()));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfiguration)
                .transactionAware()
                .build();
    }

    private static final class RedisCachePolymorphicTypeValidator extends PolymorphicTypeValidator {

        @Override
        public Validity validateBaseType(DatabindContext context, JavaType baseType) {
            return Validity.INDETERMINATE;
        }

        @Override
        public Validity validateSubClassName(DatabindContext context, JavaType baseType, String subClassName) {
            if (subClassName.startsWith("com.elevenst.realtimechat.")
                    || subClassName.startsWith("java.lang.")
                    || subClassName.startsWith("java.util.")
                    || subClassName.startsWith("java.math.")) {
                return Validity.ALLOWED;
            }
            return Validity.DENIED;
        }

        @Override
        public Validity validateSubType(DatabindContext context, JavaType baseType, JavaType subType) {
            return validateSubClassName(context, baseType, subType.getRawClass().getName());
        }
    }
}
