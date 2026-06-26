package com.elevenst.realtimechat.global.config;

import com.elevenst.realtimechat.domain.message.service.RedisChatMessagePublisher;
import com.elevenst.realtimechat.domain.message.service.RedisChatMessageSubscriber;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisPubSubConfig {

    @Bean
    public RedisSerializer<Object> chatMessageRedisSerializer() {
        return GenericJacksonJsonRedisSerializer.builder().build();
    }

    @Bean
    public RedisTemplate<String, Object> chatMessageRedisTemplate(
            RedisConnectionFactory redisConnectionFactory,
            @Qualifier("chatMessageRedisSerializer")
            RedisSerializer<Object> chatMessageRedisSerializer
    ) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(chatMessageRedisSerializer);
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(chatMessageRedisSerializer);
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.redis.pubsub", name = "enabled", havingValue = "true", matchIfMissing = true)
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory,
            RedisChatMessageSubscriber redisChatMessageSubscriber
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(redisConnectionFactory);
        container.addMessageListener(
                redisChatMessageSubscriber,
                new ChannelTopic(RedisChatMessagePublisher.CHAT_MESSAGE_CHANNEL)
        );
        return container;
    }
}
