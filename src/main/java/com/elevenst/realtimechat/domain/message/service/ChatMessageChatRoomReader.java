package com.elevenst.realtimechat.domain.message.service;

import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoom;

public interface ChatMessageChatRoomReader {
    ChatRoom getRoom(Long chatRoomId);
    ChatRoom getRoomReference(Long chatRoomId);
    void validateParticipant(Long chatRoomId, Long memberId);
}
