package com.elevenst.realtimechat.domain.message.service;

import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoom;
import com.elevenst.realtimechat.domain.message.dto.ChatMessageRequest;
import com.elevenst.realtimechat.domain.message.entity.ChatMessage;
import com.elevenst.realtimechat.domain.message.entity.MessageType;
import com.elevenst.realtimechat.domain.message.exception.ChatMessageErrorCode;
import com.elevenst.realtimechat.domain.message.exception.ChatMessageException;
import com.elevenst.realtimechat.domain.message.repository.ChatMessageRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatMessagePersistenceService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageChatRoomReader chatRoomReader;
    private final ChatMessageProductSnapshotReader productSnapshotReader;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatMessage saveNewMessage(
            Long chatRoomId,
            Long senderId,
            ChatMessageRequest request,
            MessageType messageType
    ) {
        ChatRoom room = chatRoomReader.getRoomReference(chatRoomId);
        ChatMessage message = createMessage(room, senderId, request, messageType);
        return chatMessageRepository.saveAndFlush(message);
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
}
