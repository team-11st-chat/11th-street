package com.elevenst.realtimechat.domain.message.service;

import com.elevenst.realtimechat.domain.message.entity.ChatMessage;

public interface ChatMessagePublisher {

    void publish(ChatMessage message);
}
