package com.elevenst.realtimechat.domain.message.service;

import com.elevenst.realtimechat.domain.message.dto.ChatMessageResponse;
import com.elevenst.realtimechat.domain.message.entity.ChatMessage;
import com.elevenst.realtimechat.domain.message.exception.ChatMessageErrorCode;
import com.elevenst.realtimechat.domain.message.exception.ChatMessageException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class RedisChatMessagePublisher implements ChatMessagePublisher {

    public static final String CHAT_MESSAGE_CHANNEL = "chat:messages";

    private final RedisTemplate<String, Object> redisTemplate;

    public RedisChatMessagePublisher(
            @Qualifier("chatMessageRedisTemplate") RedisTemplate<String, Object> redisTemplate
    ) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void publish(ChatMessage message) {
        try {
            redisTemplate.convertAndSend(CHAT_MESSAGE_CHANNEL, ChatMessageResponse.from(message));
        } catch (RuntimeException exception) {
            throw new ChatMessageException(ChatMessageErrorCode.PUBLISH_FAILED, exception);
        }
    }
}
