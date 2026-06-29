package com.elevenst.realtimechat.domain.message.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomParticipantRepository;
import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomRepository;
import com.elevenst.realtimechat.domain.message.repository.ChatMessageRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

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
        Long chatRoomId = 1L;
        Long cursor = 10L;
        int size = 30;
        LocalDateTime retentionStartedAt = LocalDateTime.of(2026, 5, 27, 12, 0);

        when(chatMessageRepository.findPreviousMessages(
                eq(chatRoomId),
                eq(cursor),
                eq(retentionStartedAt),
                eq(PageRequest.of(0, size + 1))
        )).thenReturn(List.of());

        chatMessageService.getPreviousMessages(chatRoomId, cursor, size);

        verify(chatMessageRepository).findPreviousMessages(
                eq(chatRoomId),
                eq(cursor),
                eq(retentionStartedAt),
                eq(PageRequest.of(0, size + 1))
        );
    }
}
