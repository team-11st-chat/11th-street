package com.elevenst.realtimechat.global.config;

import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableConfigurationProperties(RedisKeyPrefixProperties.class)
public class RedisLockConfig {

    private static final String REDIS_PROTOCOL_PREFIX = "redis://";

    @Bean(name = "redisLockRedissonConfig")
    public Config redisLockRedissonConfig(
            @Value("${spring.data.redis.host:localhost}") String host,
            @Value("${spring.data.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password,
            RedisKeyPrefixProperties redisKeyPrefixProperties
    ) {
        redisKeyPrefixProperties.validateSeparated();

        Config config = new Config();
        SingleServerConfig singleServerConfig = config.useSingleServer()
                .setAddress(REDIS_PROTOCOL_PREFIX + host + ":" + port);

        if (StringUtils.hasText(password)) {
            singleServerConfig.setPassword(password);
        }

        return config;
    }
}
