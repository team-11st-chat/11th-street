package com.elevenst.realtimechat.domain.message.entity;

import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoom;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "chat_message",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_message_composite",
                columnNames = {"chat_room_id", "sender_id", "client_message_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "client_message_id", nullable = false)
    private String clientMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_name_snapshot", length = 100)
    private String productNameSnapshot;

    @Column(name = "product_price_snapshot", precision = 10, scale = 2)
    private BigDecimal productPriceSnapshot;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    private ChatMessage(
            ChatRoom chatRoom,
            Long senderId,
            String content,
            String clientMessageId,
            MessageType messageType,
            Long productId,
            String productNameSnapshot,
            BigDecimal productPriceSnapshot,
            LocalDateTime sentAt
    ) {
        this.chatRoom = chatRoom;
        this.senderId = senderId;
        this.content = content;
        this.clientMessageId = clientMessageId;
        this.messageType = messageType;
        this.productId = productId;
        this.productNameSnapshot = productNameSnapshot;
        this.productPriceSnapshot = productPriceSnapshot;
        this.sentAt = sentAt;
    }

    public static ChatMessage text(
            ChatRoom chatRoom,
            Long senderId,
            String content,
            String clientMessageId,
            LocalDateTime sentAt
    ) {
        return new ChatMessage(chatRoom, senderId, content, clientMessageId, MessageType.TEXT, null, null, null, sentAt);
    }

    public static ChatMessage productReference(
            ChatRoom chatRoom,
            Long senderId,
            String content,
            String clientMessageId,
            Long productId,
            String productNameSnapshot,
            BigDecimal productPriceSnapshot,
            LocalDateTime sentAt
    ) {
        return new ChatMessage(
                chatRoom,
                senderId,
                content,
                clientMessageId,
                MessageType.PRODUCT_REFERENCE,
                productId,
                productNameSnapshot,
                productPriceSnapshot,
                sentAt
        );
    }

    public static ChatMessage system(ChatRoom chatRoom, String content, String clientMessageId, LocalDateTime sentAt) {
        return new ChatMessage(chatRoom, null, content, clientMessageId, MessageType.SYSTEM, null, null, null, sentAt);
    }
}
