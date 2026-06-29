package com.elevenst.realtimechat.domain.message.dto;

import com.elevenst.realtimechat.domain.message.entity.ChatMessage;
import com.elevenst.realtimechat.domain.message.entity.MessageType;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long chatRoomId,
        Long senderId,
        String content,
        String clientMessageId,
        MessageType messageType,
        ChatMessageProductResponse product,
        LocalDateTime sentAt
) {

    public static ChatMessageResponse from(ChatMessage message) {
        return new ChatMessageResponse(
                message.getId(),
                message.getChatRoom().getId(),
                message.getSenderId(),
                message.getContent(),
                message.getClientMessageId(),
                message.getMessageType(),
                productFrom(message),
                message.getSentAt()
        );
    }

    private static ChatMessageProductResponse productFrom(ChatMessage message) {
        if (message.getProductId() == null) {
            return null;
        }
        return new ChatMessageProductResponse(
                message.getProductId(),
                message.getProductNameSnapshot(),
                message.getProductPriceSnapshot()
        );
    }
}
