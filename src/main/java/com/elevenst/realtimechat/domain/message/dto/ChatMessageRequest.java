package com.elevenst.realtimechat.domain.message.dto;

import com.elevenst.realtimechat.domain.message.entity.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessageRequest(
        @NotBlank
        @Size(max = 1000)
        String content,

        @NotBlank
        @Size(max = 255)
        String clientMessageId,

        MessageType messageType,

        Long productId
) {

    public MessageType resolvedMessageType() {
        return messageType == null ? MessageType.TEXT : messageType;
    }
}
