package com.elevenst.realtimechat.domain.message.service;

import static org.mockito.Mockito.verify;

import com.elevenst.realtimechat.domain.message.dto.ChatMessageResponse;
import com.elevenst.realtimechat.domain.message.entity.MessageType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class RedisChatMessageSubscriberTest {

    @Test
    void onMessage_convertsSerializedPayloadAndBroadcastsToChatRoomTopic() {
        GenericJacksonJsonRedisSerializer serializer = GenericJacksonJsonRedisSerializer.builder().build();
        SimpMessagingTemplate messagingTemplate = org.mockito.Mockito.mock(SimpMessagingTemplate.class);
        RedisChatMessageSubscriber subscriber = new RedisChatMessageSubscriber(
                serializer,
                messagingTemplate
        );
        ChatMessageResponse response = new ChatMessageResponse(
                1L,
                10L,
                20L,
                "hello",
                "client-message-1",
                MessageType.TEXT,
                null,
                LocalDateTime.of(2026, 6, 30, 17, 40)
        );

        subscriber.onMessage(
                new DefaultMessage("chat:messages".getBytes(), serializer.serialize(response)),
                null
        );

        verify(messagingTemplate).convertAndSend("/topic/chatrooms/10", response);
    }
}
