package com.elevenst.realtimechat.domain.message.service;

import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoom;
import com.elevenst.realtimechat.domain.message.dto.ChatMessageHistoryResponse;
import com.elevenst.realtimechat.domain.message.dto.ChatMessageRequest;
import com.elevenst.realtimechat.domain.message.dto.ChatMessageResponse;
import com.elevenst.realtimechat.domain.message.entity.ChatMessage;
import com.elevenst.realtimechat.domain.message.entity.MessageType;
import com.elevenst.realtimechat.domain.message.repository.ChatMessageRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatMessageService {

    private static final int ACTIVE_MESSAGE_RETENTION_DAYS = 30;

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageChatRoomReader chatRoomReader;
    private final ChatMessagePublisher chatMessagePublisher;
    private final Clock clock;
    private final ChatMessagePersistenceService chatMessagePersistenceService;

    @Transactional(readOnly = true)
    public ChatMessageHistoryResponse getPreviousMessages(Long memberId, Long chatRoomId, Long cursor, int size) {
        ChatRoom room = getRoom(chatRoomId);
        validateParticipant(room.getId(), memberId);

        LocalDateTime retentionStartedAt = LocalDateTime.now(clock).minusDays(ACTIVE_MESSAGE_RETENTION_DAYS);
        List<ChatMessage> messages = chatMessageRepository.findPreviousMessages(
                room.getId(),
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
    public ChatMessageResponse sendMessage(Long chatRoomId, Long senderId, ChatMessageRequest request) {
        ChatRoom room = getRoom(chatRoomId);
        validateParticipant(room.getId(), senderId);

        MessageType messageType = request.resolvedMessageType();
        ChatMessage message = chatMessageRepository
                .findByChatRoomIdAndSenderIdAndClientMessageId(room.getId(), senderId, request.clientMessageId())
                .map(existingMessage -> {
                    publishAfterCommit(existingMessage);
                    return existingMessage;
                })
                .orElseGet(() -> saveNewMessage(room, senderId, request, messageType));

        return ChatMessageResponse.from(message);
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
        publishAfterCommit(message);
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
        publishAfterCommit(message);
    }

    private ChatMessage saveNewMessage(
            ChatRoom room,
            Long senderId,
            ChatMessageRequest request,
            MessageType messageType
    ) {
        try {
            ChatMessage savedMessage = chatMessagePersistenceService.saveNewMessage(
                    room.getId(),
                    senderId,
                    request,
                    messageType
            );
            publishAfterCommit(savedMessage);
            return savedMessage;
        } catch (DataIntegrityViolationException exception) {
            ChatMessage existingMessage = chatMessageRepository
                    .findByChatRoomIdAndSenderIdAndClientMessageId(
                            room.getId(),
                            senderId,
                            request.clientMessageId()
                    )
                    .orElseThrow(() -> exception);
            publishAfterCommit(existingMessage);
            return existingMessage;
        }
    }

    private ChatRoom getRoom(Long chatRoomId) {
        return chatRoomReader.getRoom(chatRoomId);
    }

    private void validateParticipant(Long chatRoomId, Long memberId) {
        chatRoomReader.validateParticipant(chatRoomId, memberId);
    }

    private void publishAfterCommit(ChatMessage message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            chatMessagePublisher.publish(message);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    chatMessagePublisher.publish(message);
                } catch (RuntimeException exception) {
                    log.warn("Failed to publish chat message after DB commit. messageId={}", message.getId(), exception);
                }
            }
        });
    }
}
