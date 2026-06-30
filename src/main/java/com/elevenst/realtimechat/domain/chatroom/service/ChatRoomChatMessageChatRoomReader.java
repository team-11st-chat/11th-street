package com.elevenst.realtimechat.domain.chatroom.service;

import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoom;
import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomErrorCode;
import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomException;
import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomParticipantRepository;
import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomRepository;
import com.elevenst.realtimechat.domain.message.service.ChatMessageChatRoomReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatRoomChatMessageChatRoomReader implements ChatMessageChatRoomReader {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository participantRepository;

    @Override
    public ChatRoom getRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    @Override
    public ChatRoom getRoomReference(Long chatRoomId) {
        return chatRoomRepository.getReferenceById(chatRoomId);
    }

    @Override
    public void validateParticipant(Long chatRoomId, Long memberId) {
        if (!participantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)) {
            throw new ChatRoomException(ChatRoomErrorCode.ACCESS_DENIED);
        }
    }
}
