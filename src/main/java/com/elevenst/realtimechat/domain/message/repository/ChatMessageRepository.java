package com.elevenst.realtimechat.domain.message.repository;

import com.elevenst.realtimechat.domain.message.entity.ChatMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Optional<ChatMessage> findByChatRoomIdAndSenderIdAndClientMessageId(
            Long chatRoomId,
            Long senderId,
            String clientMessageId
    );

    @Query("""
            select message
            from ChatMessage message
            where message.chatRoom.id = :chatRoomId
              and (:cursor is null or message.id < :cursor)
              and message.sentAt >= :retentionStartedAt
            order by message.id desc
            """)
    List<ChatMessage> findPreviousMessages(
            @Param("chatRoomId") Long chatRoomId,
            @Param("cursor") Long cursor,
            @Param("retentionStartedAt") LocalDateTime retentionStartedAt,
            Pageable pageable
    );
}
