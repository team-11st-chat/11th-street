package com.elevenst.realtimechat.domain.message.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoom;
import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomException;
import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomParticipantRepository;
import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomRepository;
import com.elevenst.realtimechat.domain.message.repository.ChatMessageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomParticipantRepository participantRepository;

    @Mock
    private ChatMessagePublisher chatMessagePublisher;

    @Mock
    private ChatMessagePersistenceService chatMessagePersistenceService;

    private ChatMessageService chatMessageService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-06-26T03:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        chatMessageService = new ChatMessageService(
                chatMessageRepository,
                chatRoomRepository,
                participantRepository,
                chatMessagePublisher,
                clock,
                chatMessagePersistenceService
        );
    }

    @Test
    void getPreviousMessages_usesThirtyDayRetentionWindow() {
        Long memberId = 2L;
        Long chatRoomId = 1L;
        Long cursor = 10L;
        int size = 30;
        LocalDateTime retentionStartedAt = LocalDateTime.of(2026, 5, 27, 12, 0);
        ChatRoom room = createRoom(chatRoomId, memberId, 3L);

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(room));
        when(participantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(room.getId(), memberId))
                .thenReturn(true);
        when(chatMessageRepository.findPreviousMessages(
                eq(room.getId()),
                eq(cursor),
                eq(retentionStartedAt),
                eq(PageRequest.of(0, size + 1))
        )).thenReturn(List.of());

        chatMessageService.getPreviousMessages(memberId, chatRoomId, cursor, size);

        verify(chatMessageRepository).findPreviousMessages(
                eq(room.getId()),
                eq(cursor),
                eq(retentionStartedAt),
                eq(PageRequest.of(0, size + 1))
        );
    }

    @Test
    void getPreviousMessages_rejectsNonParticipant() {
        Long memberId = 2L;
        Long chatRoomId = 1L;
        ChatRoom room = createRoom(chatRoomId, 3L, 4L);

        when(chatRoomRepository.findById(chatRoomId)).thenReturn(Optional.of(room));
        when(participantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(room.getId(), memberId))
                .thenReturn(false);

        assertThatThrownBy(() -> chatMessageService.getPreviousMessages(memberId, chatRoomId, null, 30))
                .isInstanceOf(ChatRoomException.class)
                .hasMessage("Chat room access is denied.");

        verify(chatMessageRepository, never()).findPreviousMessages(
                any(),
                any(),
                any(),
                any()
        );
    }

    private ChatRoom createRoom(Long chatRoomId, Long buyerId, Long sellerId) {
        ChatRoom room = ChatRoom.product(buyerId, sellerId);
        ReflectionTestUtils.setField(room, "id", chatRoomId);
        return room;
    }
}
