package com.elevenst.realtimechat.domain.message.service;

import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoom;
import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomErrorCode;
import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomException;
import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomParticipantRepository;
import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomRepository;
import com.elevenst.realtimechat.domain.message.dto.ChatMessageHistoryResponse;
import com.elevenst.realtimechat.domain.message.dto.ChatMessageRequest;
import com.elevenst.realtimechat.domain.message.dto.ChatMessageResponse;
import com.elevenst.realtimechat.domain.message.entity.ChatMessage;
import com.elevenst.realtimechat.domain.message.entity.MessageType;
import com.elevenst.realtimechat.domain.message.exception.ChatMessageErrorCode;
import com.elevenst.realtimechat.domain.message.exception.ChatMessageException;
import com.elevenst.realtimechat.domain.message.repository.ChatMessageRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository participantRepository;
    private final ChatMessagePublisher chatMessagePublisher;
    private final ChatMessageProductSnapshotReader productSnapshotReader;

    @Transactional(readOnly = true)
    public ChatMessageHistoryResponse getPreviousMessages(Long chatRoomId, Long cursor, int size) {
        List<ChatMessage> messages = chatMessageRepository.findPreviousMessages(
                chatRoomId,
                cursor,
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
        ChatMessage message = createMessage(room, senderId, request, messageType);
        try {
            ChatMessage savedMessage = chatMessageRepository.saveAndFlush(message);
            publishAfterCommit(savedMessage);
            return savedMessage;
        } catch (DataIntegrityViolationException exception) {
            return chatMessageRepository
                    .findByChatRoomIdAndSenderIdAndClientMessageId(
                            room.getId(),
                            senderId,
                            request.clientMessageId()
                    )
                    .orElseThrow(() -> exception);
        }
    }

    private ChatMessage createMessage(
            ChatRoom room,
            Long senderId,
            ChatMessageRequest request,
            MessageType messageType
    ) {
        LocalDateTime now = LocalDateTime.now();
        if (messageType == MessageType.TEXT) {
            return ChatMessage.text(room, senderId, request.content(), request.clientMessageId(), now);
        }
        if (messageType == MessageType.PRODUCT_REFERENCE) {
            if (request.productId() == null) {
                throw new ChatMessageException(ChatMessageErrorCode.PRODUCT_REFERENCE_REQUIRED);
            }
            ChatMessageProductSnapshot product = productSnapshotReader.getSnapshot(request.productId());
            return ChatMessage.productReference(
                    room,
                    senderId,
                    request.content(),
                    request.clientMessageId(),
                    product.id(),
                    product.name(),
                    product.price(),
                    now
            );
        }
        throw new ChatMessageException(ChatMessageErrorCode.INVALID_MESSAGE_TYPE);
    }

    private ChatRoom getRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private void validateParticipant(Long chatRoomId, Long memberId) {
        if (!participantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)) {
            throw new ChatRoomException(ChatRoomErrorCode.ACCESS_DENIED);
        }
    }

    private void publishAfterCommit(ChatMessage message) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            chatMessagePublisher.publish(message);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                chatMessagePublisher.publish(message);
            }
        });
    }
}
