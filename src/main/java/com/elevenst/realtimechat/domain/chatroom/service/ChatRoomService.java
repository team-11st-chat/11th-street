package com.elevenst.realtimechat.domain.chatroom.service;

import com.elevenst.realtimechat.domain.chatroom.dto.ChatRoomResponse;
import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoom;
import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoomParticipant;
import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoomType;
import com.elevenst.realtimechat.domain.chatroom.entity.CsStatus;
import com.elevenst.realtimechat.domain.chatroom.entity.ParticipantRole;
import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomErrorCode;
import com.elevenst.realtimechat.domain.chatroom.exception.ChatRoomException;
import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomParticipantRepository;
import com.elevenst.realtimechat.domain.chatroom.repository.ChatRoomRepository;
import com.elevenst.realtimechat.domain.member.entity.MemberRole;
import com.elevenst.realtimechat.domain.member.service.MemberQueryService;
import com.elevenst.realtimechat.domain.message.dto.ChatMessageHistoryResponse;
import com.elevenst.realtimechat.domain.message.service.ChatMessageService;
import com.elevenst.realtimechat.global.support.LockManager;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private static final List<CsStatus> ACTIVE_CS_STATUSES = List.of(CsStatus.WAITING, CsStatus.IN_PROGRESS);
    private static final int MAX_MESSAGE_HISTORY_SIZE = 100;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository participantRepository;
    private final MemberQueryService memberQueryService;
    private final ChatRoomProductCatalogReader productCatalogReader;
    private final ChatMessageService chatMessageService;
    private final LockManager lockManager;

    @Transactional
    public ChatRoomResponse createProductRoom(Long memberId, Long productId) {
        validateMemberExists(memberId);
        ProductSellerSnapshot product = productCatalogReader.getProductSeller(productId);
        validateProductRoomRequester(memberId, product.sellerId());

        String lockKey = "lock:chatroom:product:" + product.sellerId() + ":" + memberId;
        acquireLock(lockKey);
        try {
            ChatRoom room = chatRoomRepository
                    .findByRoomTypeAndSellerIdAndCreatedByMemberId(ChatRoomType.PRODUCT, product.sellerId(), memberId)
                    .orElseGet(() -> saveProductRoom(memberId, product.sellerId()));

            return ChatRoomResponse.from(room);
        } finally {
            lockManager.unlock(lockKey);
        }
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getProductRooms(Long memberId) {
        return chatRoomRepository.findProductRoomsByMemberId(memberId)
                .stream()
                .map(ChatRoomResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatRoomResponse getProductRoom(Long memberId, Long chatRoomId) {
        ChatRoom room = getRoom(chatRoomId, ChatRoomType.PRODUCT);
        validateParticipant(room, memberId);
        return ChatRoomResponse.from(room);
    }

    @Transactional(readOnly = true)
    public ChatMessageHistoryResponse getMessages(Long memberId, Long chatRoomId, Long cursor, int size) {
        validateMessageHistorySize(size);
        return chatMessageService.getPreviousMessages(memberId, chatRoomId, cursor, size);
    }

    @Transactional
    public ChatRoomResponse join(Long memberId, Long chatRoomId) {
        validateMemberExists(memberId);

        ChatRoom room = getRoom(chatRoomId);
        LocalDateTime now = LocalDateTime.now();
        participantRepository.findByChatRoomIdAndMemberId(room.getId(), memberId)
                .map(existingParticipant -> rejoin(room, existingParticipant, now))
                .orElseGet(() -> createParticipant(room, memberId, resolveJoinParticipantRole(room, memberId), now));

        chatMessageService.recordParticipantJoined(room, memberId, now);
        return ChatRoomResponse.from(room);
    }

    @Transactional
    public ChatRoomResponse leave(Long memberId, Long chatRoomId) {
        ChatRoom room = getRoom(chatRoomId);
        ChatRoomParticipant participant = participantRepository.findByChatRoomIdAndMemberId(room.getId(), memberId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorCode.NOT_JOINED));
        if (participant.getLeftAt() != null) {
            throw new ChatRoomException(ChatRoomErrorCode.NOT_JOINED);
        }

        LocalDateTime now = LocalDateTime.now();
        participant.leave(now);
        chatMessageService.recordParticipantLeft(room, memberId, now);
        return ChatRoomResponse.from(room);
    }

    @Transactional
    public ChatRoomResponse createCsRoom(Long memberId) {
        validateMemberExists(memberId);

        String lockKey = "lock:chatroom:cs:member:" + memberId;
        acquireLock(lockKey);
        try {
            if (chatRoomRepository.existsByRoomTypeAndCreatedByMemberIdAndCsStatusIn(
                    ChatRoomType.CS, memberId, ACTIVE_CS_STATUSES)) {
                throw new ChatRoomException(ChatRoomErrorCode.ACTIVE_CS_ROOM_EXISTS);
            }

            LocalDateTime now = LocalDateTime.now();
            ChatRoom room = chatRoomRepository.save(ChatRoom.cs(memberId));
            participantRepository.save(ChatRoomParticipant.join(room, memberId, ParticipantRole.CUSTOMER, now));
            return ChatRoomResponse.from(room);
        } finally {
            lockManager.unlock(lockKey);
        }
    }

    @Transactional(readOnly = true)
    public List<ChatRoomResponse> getCsRooms(Long memberId, MemberRole role) {
        boolean admin = isCsAdmin(role);
        return chatRoomRepository.findCsRooms(memberId, admin)
                .stream()
                .map(ChatRoomResponse::from)
                .toList();
    }

    @Transactional
    public ChatRoomResponse acceptCsRoom(Long memberId, MemberRole role, Long chatRoomId) {
        validateCsAdmin(role);
        validateMemberExists(memberId);

        ChatRoom room = getRoomWithLock(chatRoomId, ChatRoomType.CS);
        room.acceptCs();
        addParticipantIfAbsent(room, memberId, ParticipantRole.CS_ADMIN, LocalDateTime.now());
        return ChatRoomResponse.from(room);
    }

    @Transactional
    public ChatRoomResponse completeCsRoom(Long memberId, MemberRole role, Long chatRoomId) {
        validateCsAdmin(role);
        ChatRoom room = getRoomWithLock(chatRoomId, ChatRoomType.CS);
        validateParticipant(room, memberId);
        room.completeCs(LocalDateTime.now());
        return ChatRoomResponse.from(room);
    }

    private ChatRoom saveProductRoom(Long memberId, Long sellerId) {
        LocalDateTime now = LocalDateTime.now();
        ChatRoom room = chatRoomRepository.save(ChatRoom.product(memberId, sellerId));
        participantRepository.save(ChatRoomParticipant.join(room, memberId, ParticipantRole.BUYER, now));
        participantRepository.save(ChatRoomParticipant.join(room, sellerId, ParticipantRole.SELLER, now));
        return room;
    }

    private ChatRoom getRoom(Long chatRoomId, ChatRoomType roomType) {
        return chatRoomRepository.findByIdAndRoomType(chatRoomId, roomType)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private ChatRoom getRoom(Long chatRoomId) {
        return chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private ChatRoom getRoomWithLock(Long chatRoomId, ChatRoomType roomType) {
        return chatRoomRepository.findByIdAndRoomTypeWithLock(chatRoomId, roomType)
                .orElseThrow(() -> new ChatRoomException(ChatRoomErrorCode.CHAT_ROOM_NOT_FOUND));
    }

    private void addParticipantIfAbsent(ChatRoom room, Long memberId, ParticipantRole role, LocalDateTime now) {
        if (!participantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(room.getId(), memberId)) {
            participantRepository.findByChatRoomIdAndMemberId(room.getId(), memberId)
                    .ifPresentOrElse(
                            participant -> participant.rejoin(now),
                            () -> createParticipant(room, memberId, role, now)
                    );
        }
    }

    private ChatRoomParticipant createParticipant(ChatRoom room, Long memberId, ParticipantRole role, LocalDateTime now) {
        return participantRepository.save(ChatRoomParticipant.join(room, memberId, role, now));
    }

    private ChatRoomParticipant rejoin(ChatRoom room, ChatRoomParticipant participant, LocalDateTime now) {
        if (participant.getLeftAt() == null) {
            throw new ChatRoomException(ChatRoomErrorCode.ALREADY_JOINED);
        }
        resolveJoinParticipantRole(room, participant.getMemberId());
        participant.rejoin(now);
        return participant;
    }

    private ParticipantRole resolveJoinParticipantRole(ChatRoom room, Long memberId) {
        if (room.isProductRoom()) {
            if (room.getCreatedByMemberId().equals(memberId)) {
                return ParticipantRole.BUYER;
            }
            if (room.getSellerId().equals(memberId)) {
                return ParticipantRole.SELLER;
            }
            throw new ChatRoomException(ChatRoomErrorCode.ACCESS_DENIED);
        }
        if (room.isCsRoom() && room.getCreatedByMemberId().equals(memberId)) {
            return ParticipantRole.CUSTOMER;
        }
        throw new ChatRoomException(ChatRoomErrorCode.ACCESS_DENIED);
    }

    private void validateParticipant(ChatRoom room, Long memberId) {
        if (!participantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(room.getId(), memberId)) {
            throw new ChatRoomException(ChatRoomErrorCode.ACCESS_DENIED);
        }
    }

    private void validateMemberExists(Long memberId) {
        if (!memberQueryService.existsById(memberId)) {
            throw new ChatRoomException(ChatRoomErrorCode.INVALID_MEMBER);
        }
    }

    @Transactional(readOnly = true)
    public void validateParticipantExistence(Long chatRoomId, Long memberId) {
        if (!participantRepository.existsByChatRoomIdAndMemberIdAndLeftAtIsNull(chatRoomId, memberId)) {
            throw new ChatRoomException(ChatRoomErrorCode.ACCESS_DENIED);
        }
    }

    private void validateProductRoomRequester(Long memberId, Long sellerId) {
        if (memberId.equals(sellerId)) {
            throw new ChatRoomException(ChatRoomErrorCode.SELF_PRODUCT_CHAT_ROOM_NOT_ALLOWED);
        }
    }

    private void acquireLock(String lockKey) {
        if (!lockManager.tryLock(lockKey)) {
            throw new ChatRoomException(ChatRoomErrorCode.LOCK_UNAVAILABLE);
        }
    }

    private void validateMessageHistorySize(int size) {
        if (size <= 0 || size > MAX_MESSAGE_HISTORY_SIZE) {
            throw new ChatRoomException(ChatRoomErrorCode.INVALID_MESSAGE_HISTORY_SIZE);
        }
    }

    private void validateCsAdmin(MemberRole role) {
        if (!isCsAdmin(role)) {
            throw new ChatRoomException(ChatRoomErrorCode.CS_ADMIN_REQUIRED);
        }
    }

    private boolean isCsAdmin(MemberRole role) {
        return role == MemberRole.CS_ADMIN || role == MemberRole.SUPER_ADMIN;
    }
}
