package com.elevenst.realtimechat.global.config;

import com.elevenst.realtimechat.domain.message.service.RedisChatMessagePublisher;
import com.elevenst.realtimechat.domain.message.service.RedisChatMessageSubscriber;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
@RequiredArgsConstructor
public class RedisPubSubConfig {

    private final RedisChatMessageSubscriber redisChatMessageSubscriber;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory redisConnectionFactory
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
