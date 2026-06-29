package com.elevenst.realtimechat.domain.message.service;

import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoom;
import com.elevenst.realtimechat.domain.message.dto.ChatMessageHistoryResponse;
import com.elevenst.realtimechat.domain.message.dto.ChatMessageResponse;
import com.elevenst.realtimechat.domain.message.entity.ChatMessage;
import com.elevenst.realtimechat.domain.message.repository.ChatMessageRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private static final int ACTIVE_MESSAGE_RETENTION_DAYS = 30;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessagePublisher chatMessagePublisher;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ChatMessageHistoryResponse getPreviousMessages(Long chatRoomId, Long cursor, int size) {
        LocalDateTime retentionStartedAt = LocalDateTime.now(clock).minusDays(ACTIVE_MESSAGE_RETENTION_DAYS);
        List<ChatMessage> messages = chatMessageRepository.findPreviousMessages(
                chatRoomId,
                cursor,
                retentionStartedAt,
                PageRequest.of(0, size + 1)
        );
        boolean hasNext = messages.size() > size;
        List<ChatMessageResponse> content = messages.stream()
                .limit(size)
                .map(ChatMessageResponse::from)
                .toList();

        return ChatMessageHistoryResponse.of(content, hasNext);
    }

    @Transactional
    public void recordParticipantJoined(ChatRoom room, Long memberId, LocalDateTime joinedAt) {
        ChatMessage message = ChatMessage.system(
                room,
                "Member " + memberId + " joined the chat room.",
                "system:participant-joined:" + memberId + ":" + UUID.randomUUID(),
                joinedAt
        );
        chatMessageRepository.save(message);
        chatMessagePublisher.publish(message);
    }

    @Transactional
    public void recordParticipantLeft(ChatRoom room, Long memberId, LocalDateTime leftAt) {
        ChatMessage message = ChatMessage.system(
                room,
                "Member " + memberId + " left the chat room.",
                "system:participant-left:" + memberId + ":" + UUID.randomUUID(),
                leftAt
        );
        chatMessageRepository.save(message);
        chatMessagePublisher.publish(message);
    }
}
