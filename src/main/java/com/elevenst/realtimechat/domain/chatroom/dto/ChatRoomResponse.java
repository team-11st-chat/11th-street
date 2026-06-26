package com.elevenst.realtimechat.domain.chatroom.dto;

import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoom;
import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoomType;
import com.elevenst.realtimechat.domain.chatroom.entity.CsStatus;
import java.time.LocalDateTime;

public record ChatRoomResponse(
        Long id,
        ChatRoomType roomType,
        Long sellerId,
        Long createdByMemberId,
        CsStatus csStatus,
        LocalDateTime closedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ChatRoomResponse from(ChatRoom room) {
        return new ChatRoomResponse(
                room.getId(),
                room.getRoomType(),
                room.getSellerId(),
                room.getCreatedByMemberId(),
                room.getCsStatus(),
                room.getClosedAt(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }
}
