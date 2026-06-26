package com.elevenst.realtimechat.domain.chatroom.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elevenst.realtimechat.domain.chatroom.dto.ChatRoomResponse;
import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoom;
import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoomType;
import com.elevenst.realtimechat.domain.chatroom.entity.CsStatus;
import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomException;
import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomParticipantRepository;
import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomRepository;
import com.elevenst.realtimechat.domain.member.entity.Member;
import com.elevenst.realtimechat.domain.member.repository.MemberRepository;
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

    private ChatRoomService chatRoomService;

    @BeforeEach
    void setUp() {
        chatRoomService = new ChatRoomService(
                chatRoomRepository,
                participantRepository,
                memberRepository,
                productCatalogReader
        );
    }

    @Test
    void createProductRoom_reusesExistingRoomForSameBuyerAndSeller() {
        Long buyerId = 1L;
        Long sellerId = 2L;
        Long productId = 100L;
        ChatRoom existingRoom = ChatRoom.product(buyerId, sellerId);

        when(memberRepository.findWithLockById(buyerId)).thenReturn(Optional.of(Member.create(
                "buyer@example.com",
                "password",
                "buyer"
        )));
        when(productCatalogReader.getProductSeller(productId))
                .thenReturn(new ProductSellerSnapshot(productId, sellerId));
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
    }

    @Test
    void createCsRoom_rejectsWhenMemberAlreadyHasActiveCsRoom() {
        Long memberId = 1L;

        when(memberRepository.findWithLockById(memberId)).thenReturn(Optional.of(Member.create(
                "customer@example.com",
                "password",
                "customer"
        )));
        when(chatRoomRepository.existsByRoomTypeAndCreatedByMemberIdAndCsStatusIn(
                ChatRoomType.CS,
                memberId,
                List.of(CsStatus.WAITING, CsStatus.IN_PROGRESS)
        )).thenReturn(true);

        assertThatThrownBy(() -> chatRoomService.createCsRoom(memberId))
                .isInstanceOf(ChatRoomException.class)
                .hasMessage("An active CS chat room already exists.");

        verify(chatRoomRepository, never()).save(any(ChatRoom.class));
    }
}
