package com.elevenst.realtimechat.domain.message.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoom;
import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomRepository;
import com.elevenst.realtimechat.domain.message.entity.ChatMessage;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
@Tag("integration")
class ChatMessageRepositoryTest {

    @Autowired
    private ChatRoomRepository chatRoomRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Test
    void findPreviousMessages_returnsOnlyMessagesWithinRetentionWindow() {
        ChatRoom room = chatRoomRepository.save(ChatRoom.product(1L, 2L));
        LocalDateTime now = LocalDateTime.of(2026, 6, 26, 12, 0);
        LocalDateTime retentionStartedAt = now.minusDays(30);

        ChatMessage oldMessage = chatMessageRepository.save(ChatMessage.system(
                room,
                "old",
                "system:old",
                retentionStartedAt.minusSeconds(1)
        ));
        ChatMessage boundaryMessage = chatMessageRepository.save(ChatMessage.system(
                room,
                "boundary",
                "system:boundary",
                retentionStartedAt
        ));
        ChatMessage recentMessage = chatMessageRepository.save(ChatMessage.system(
                room,
                "recent",
                "system:recent",
                now
        ));

        List<ChatMessage> messages = chatMessageRepository.findPreviousMessages(
                room.getId(),
                null,
                retentionStartedAt,
                PageRequest.of(0, 10)
        );

        assertThat(messages)
                .extracting(ChatMessage::getId)
                .containsExactly(recentMessage.getId(), boundaryMessage.getId())
                .doesNotContain(oldMessage.getId());
    }

    @Test
    void findPreviousMessages_appliesCursorAfterRetentionWindow() {
        ChatRoom room = chatRoomRepository.save(ChatRoom.product(1L, 2L));
        LocalDateTime now = LocalDateTime.of(2026, 6, 26, 12, 0);
        LocalDateTime retentionStartedAt = now.minusDays(30);

        ChatMessage retainedMessage = chatMessageRepository.save(ChatMessage.system(
                room,
                "retained",
                "system:retained",
                retentionStartedAt
        ));
        ChatMessage cursorMessage = chatMessageRepository.save(ChatMessage.system(
                room,
                "cursor",
                "system:cursor",
                now
        ));

        List<ChatMessage> messages = chatMessageRepository.findPreviousMessages(
                room.getId(),
                cursorMessage.getId(),
                retentionStartedAt,
                PageRequest.of(0, 10)
        );

        assertThat(messages)
                .extracting(ChatMessage::getId)
                .containsExactly(retainedMessage.getId());
    }
}
