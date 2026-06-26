package com.elevenst.realtimechat.domain.chatroom.entity;

import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomErrorCode;
import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomException;
import com.elevenst.realtimechat.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "chat_room",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_chat_room_composite",
                columnNames = {"room_type", "seller_id", "created_by_member_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private ChatRoomType roomType;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "created_by_member_id", nullable = false)
    private Long createdByMemberId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cs_status", length = 20)
    private CsStatus csStatus;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    private ChatRoom(ChatRoomType roomType, Long sellerId, Long createdByMemberId, CsStatus csStatus) {
        this.roomType = roomType;
        this.sellerId = sellerId;
        this.createdByMemberId = createdByMemberId;
        this.csStatus = csStatus;
    }

    public static ChatRoom product(Long createdByMemberId, Long sellerId) {
        validateMemberId(createdByMemberId);
        validateMemberId(sellerId);
        return new ChatRoom(ChatRoomType.PRODUCT, sellerId, createdByMemberId, null);
    }

    public static ChatRoom cs(Long createdByMemberId) {
        validateMemberId(createdByMemberId);
        return new ChatRoom(ChatRoomType.CS, null, createdByMemberId, CsStatus.WAITING);
    }

    public void acceptCs() {
        validateCsRoom();
        if (this.csStatus != CsStatus.WAITING) {
            throw new ChatRoomException(ChatRoomErrorCode.CS_ROOM_NOT_WAITING);
        }
        this.csStatus = CsStatus.IN_PROGRESS;
    }

    public void completeCs(LocalDateTime completedAt) {
        validateCsRoom();
        if (this.csStatus != CsStatus.IN_PROGRESS) {
            throw new ChatRoomException(ChatRoomErrorCode.CS_ROOM_NOT_IN_PROGRESS);
        }
        this.csStatus = CsStatus.COMPLETED;
        this.closedAt = completedAt;
    }

    public boolean isProductRoom() {
        return this.roomType == ChatRoomType.PRODUCT;
    }

    public boolean isCsRoom() {
        return this.roomType == ChatRoomType.CS;
    }

    public boolean isActiveCsRoom() {
        return isCsRoom() && this.csStatus != CsStatus.COMPLETED;
    }

    private void validateCsRoom() {
        if (!isCsRoom()) {
            throw new ChatRoomException(ChatRoomErrorCode.INVALID_ROOM_TYPE);
        }
    }

    private static void validateMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new ChatRoomException(ChatRoomErrorCode.INVALID_MEMBER);
        }
    }
}
