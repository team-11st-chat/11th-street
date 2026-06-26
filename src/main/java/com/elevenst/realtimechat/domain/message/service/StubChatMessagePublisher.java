package com.elevenst.realtimechat.domain.message.service;

import com.elevenst.realtimechat.domain.message.entity.ChatMessage;
import org.springframework.stereotype.Component;

@Component
public class StubChatMessagePublisher implements ChatMessagePublisher {

    @Override
    public void publish(ChatMessage message) {
        // 다른 Issue 구현 완료 후 WebSocket/Redis PubSub 실제 구현체로 교체 예정.
    }
}
