package com.elevenst.realtimechat.domain.message.service;

import com.elevenst.realtimechat.domain.message.dto.ChatMessageProductResponse;
import com.elevenst.realtimechat.domain.message.dto.ChatMessageResponse;
import com.elevenst.realtimechat.domain.message.entity.MessageType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisChatMessageSubscriber implements MessageListener {

    private static final String CHATROOM_TOPIC_PREFIX = "/topic/chatrooms/";

    private final RedisSerializer<Object> messageSerializer;
    private final SimpMessagingTemplate messagingTemplate;

    public RedisChatMessageSubscriber(
            @Qualifier("chatMessageRedisSerializer") RedisSerializer<Object> messageSerializer,
            SimpMessagingTemplate messagingTemplate
    ) {
        this.messageSerializer = messageSerializer;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            ChatMessageResponse response = deserializeChatMessage(messageSerializer.deserialize(message.getBody()));
            messagingTemplate.convertAndSend(CHATROOM_TOPIC_PREFIX + response.chatRoomId(), response);
        } catch (IllegalArgumentException | SerializationException exception) {
            log.warn("Failed to deserialize chat message from Redis Pub/Sub.", exception);
        }
    }

    private ChatMessageResponse deserializeChatMessage(Object payload) {
        if (payload instanceof ChatMessageResponse response) {
            return response;
        }
        if (!(payload instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Unsupported chat message payload type: " + payload);
        }

        return new ChatMessageResponse(
                toLong(map.get("id")),
                toLong(map.get("chatRoomId")),
                toLong(map.get("senderId")),
                (String) map.get("content"),
                (String) map.get("clientMessageId"),
                MessageType.valueOf((String) map.get("messageType")),
                toProductResponse(map.get("product")),
                toLocalDateTime(map.get("sentAt"))
        );
    }

    private ChatMessageProductResponse toProductResponse(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof ChatMessageProductResponse response) {
            return response;
        }
        if (!(payload instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Unsupported product payload type: " + payload);
        }
        return new ChatMessageProductResponse(
                toLong(map.get("id")),
                (String) map.get("name"),
                toBigDecimal(map.get("price"))
        );
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }

        return Long.valueOf(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal;
        }

        return new BigDecimal(value.toString());
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }

        return LocalDateTime.parse(value.toString());
    }
}
