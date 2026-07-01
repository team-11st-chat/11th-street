package com.elevenst.realtimechat.domain.chatroom.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductChatRoomCreateRequest(
        @NotNull
        @Positive
        Long productId
) {
}
