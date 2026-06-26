package com.elevenst.realtimechat.domain.message.service;

import com.elevenst.realtimechat.domain.message.dto.ChatMessageResponse;
import com.elevenst.realtimechat.domain.message.entity.ChatMessage;
import com.elevenst.realtimechat.domain.message.exception.ChatMessageErrorCode;
import com.elevenst.realtimechat.domain.message.exception.ChatMessageException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisChatMessagePublisher implements ChatMessagePublisher {

    public static final String CHAT_MESSAGE_CHANNEL = "chat:messages";
    private static final RedisSerializer<Object> MESSAGE_SERIALIZER =
            GenericJacksonJsonRedisSerializer.builder().build();

    private final StringRedisTemplate redisTemplate;

    @Override
    public void publish(ChatMessage message) {
        try {
            byte[] serializedMessage = MESSAGE_SERIALIZER.serialize(ChatMessageResponse.from(message));
            String payload = new String(serializedMessage, StandardCharsets.UTF_8);
            redisTemplate.convertAndSend(CHAT_MESSAGE_CHANNEL, payload);
        } catch (SerializationException exception) {
            throw new ChatMessageException(ChatMessageErrorCode.PUBLISH_FAILED, exception);
        } catch (RuntimeException exception) {
            throw new ChatMessageException(ChatMessageErrorCode.PUBLISH_FAILED, exception);
        }
    }
}
