package com.elevenst.realtimechat.domain.chatroom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elevenst.realtimechat.domain.chatroom.dto.ChatRoomResponse;
import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoom;
import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoomParticipant;
import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoomType;
import com.elevenst.realtimechat.domain.chatroom.entity.CsStatus;
import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomException;
import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomParticipantRepository;
import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomRepository;
import com.elevenst.realtimechat.domain.member.entity.MemberRole;
import com.elevenst.realtimechat.domain.member.repository.MemberRepository;
import com.elevenst.realtimechat.global.support.LockManager;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomParticipantRepository participantRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ChatRoomProductCatalogReader productCatalogReader;

    @Mock
    private LockManager lockManager;

    private ChatRoomService chatRoomService;

    @BeforeEach
    void setUp() {
        chatRoomService = new ChatRoomService(
                chatRoomRepository,
                participantRepository,
                memberRepository,
                productCatalogReader,
                lockManager
        );
    }

    @Test
    void createProductRoom_reusesExistingRoomForSameBuyerAndSeller() {
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long productId = 100L;
        ChatRoom existingRoom = ChatRoom.product(buyerId, sellerId);

        when(memberRepository.existsById(buyerId)).thenReturn(true);
        when(productCatalogReader.getProductSeller(productId))
                .thenReturn(new ProductSellerSnapshot(productId, sellerId));
        when(lockManager.tryLock("lock:chatroom:product:" + sellerId + ":" + buyerId)).thenReturn(true);
        when(chatRoomRepository.findByRoomTypeAndSellerIdAndCreatedByMemberId(
                ChatRoomType.PRODUCT,
                sellerId,
                buyerId
        )).thenReturn(Optional.of(existingRoom));

        ChatRoomResponse response = chatRoomService.createProductRoom(buyerId, productId);

        assertThat(response.roomType()).isEqualTo(ChatRoomType.PRODUCT);
        assertThat(response.sellerId()).isEqualTo(sellerId);
        assertThat(response.createdByMemberId()).isEqualTo(buyerId);
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        verify(lockManager).unlock("lock:chatroom:product:" + sellerId + ":" + buyerId);
    }

    @Test
    void createProductRoom_rejectsSellerOwnProductRoom() {
        Long sellerId = 2L;
        Long productId = 100L;

        when(memberRepository.existsById(sellerId)).thenReturn(true);
        when(productCatalogReader.getProductSeller(productId))
                .thenReturn(new ProductSellerSnapshot(productId, sellerId));

        assertThatThrownBy(() -> chatRoomService.createProductRoom(sellerId, productId))
                .isInstanceOf(ChatRoomException.class)
                .hasMessage("Cannot create a chat room with yourself.");

        verify(lockManager, never()).tryLock(any());
        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }

    @Test
    void createProductRoom_savesParticipantsWithoutDuplicateLookupWhenRoomIsNew() {
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long productId = 100L;
        ChatRoom newRoom = ChatRoom.product(buyerId, sellerId);

        when(memberRepository.existsById(buyerId)).thenReturn(true);
        when(productCatalogReader.getProductSeller(productId))
                .thenReturn(new ProductSellerSnapshot(productId, sellerId));
        when(lockManager.tryLock("lock:chatroom:product:" + sellerId + ":" + buyerId)).thenReturn(true);
        when(chatRoomRepository.findByRoomTypeAndSellerIdAndCreatedByMemberId(
                ChatRoomType.PRODUCT,
                sellerId,
                buyerId
        )).thenReturn(Optional.empty());
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(newRoom);

        ChatRoomResponse response = chatRoomService.createProductRoom(buyerId, productId);

        assertThat(response.sellerId()).isEqualTo(sellerId);
        verify(participantRepository, never()).existsByChatRoomIdAndMemberIdAndLeftAtIsNull(any(), eq(sellerId));
        verify(participantRepository, times(2)).save(any(ChatRoomParticipant.class));
    }

    @Test
    void createCsRoom_rejectsWhenMemberAlreadyHasActiveCsRoom() {
        Long memberId = 1L;

        when(memberRepository.existsById(memberId)).thenReturn(true);
        when(lockManager.tryLock("lock:chatroom:cs:member:" + memberId)).thenReturn(true);
        when(chatRoomRepository.existsByRoomTypeAndCreatedByMemberIdAndCsStatusIn(
                ChatRoomType.CS,
                memberId,
                List.of(CsStatus.WAITING, CsStatus.IN_PROGRESS)
        )).thenReturn(true);

        assertThatThrownBy(() -> chatRoomService.createCsRoom(memberId))
                .isInstanceOf(ChatRoomException.class)
                .hasMessage("An active CS chat room already exists.");

        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
        verify(lockManager).unlock("lock:chatroom:cs:member:" + memberId);
    }

    @Test
    void acceptCsRoom_usesLockedRoomLookupBeforeStatusChange() {
        Long adminId = 3L;
        Long chatRoomId = 10L;
        ChatRoom waitingRoom = ChatRoom.cs(1L);

        when(memberRepository.existsById(adminId)).thenReturn(true);
        when(chatRoomRepository.findByIdAndRoomTypeWithLock(chatRoomId, ChatRoomType.CS))
                .thenReturn(Optional.of(waitingRoom));

        ChatRoomResponse response = chatRoomService.acceptCsRoom(adminId, MemberRole.CS_ADMIN, chatRoomId);

        assertThat(response.csStatus()).isEqualTo(CsStatus.IN_PROGRESS);
        verify(chatRoomRepository).findByIdAndRoomTypeWithLock(chatRoomId, ChatRoomType.CS);
        verify(chatRoomRepository, never()).findByIdAndRoomType(chatRoomId, ChatRoomType.CS);
    }

    @Test
    void completeCs_rejectsWaitingRoom() {
        ChatRoom waitingRoom = ChatRoom.cs(1L);

        assertThatThrownBy(() -> waitingRoom.completeCs(LocalDateTime.now()))
                .isInstanceOf(ChatRoomException.class)
                .hasMessage("CS chat room is not in progress.");
    }
}
