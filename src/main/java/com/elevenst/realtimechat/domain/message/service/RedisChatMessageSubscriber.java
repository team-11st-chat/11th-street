package com.elevenst.realtimechat.domain.message.service;

import com.elevenst.realtimechat.domain.message.dto.ChatMessageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisChatMessageSubscriber implements MessageListener {

    private static final String CHATROOM_TOPIC_PREFIX = "/topic/chatrooms/";
    private static final RedisSerializer<Object> MESSAGE_SERIALIZER =
            GenericJacksonJsonRedisSerializer.builder().build();

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            ChatMessageResponse response = (ChatMessageResponse) MESSAGE_SERIALIZER.deserialize(message.getBody());
            messagingTemplate.convertAndSend(CHATROOM_TOPIC_PREFIX + response.chatRoomId(), response);
        } catch (SerializationException | ClassCastException exception) {
            log.warn("Failed to deserialize chat message from Redis Pub/Sub.", exception);
        }
    }
}
