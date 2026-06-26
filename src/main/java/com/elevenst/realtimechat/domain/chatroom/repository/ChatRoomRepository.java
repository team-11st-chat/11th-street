package com.elevenst.realtimechat.domain.chatroom.repository;

import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoom;
import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoomType;
import com.elevenst.realtimechat.domain.chatroom.entity.CsStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByRoomTypeAndSellerIdAndCreatedByMemberId(
            ChatRoomType roomType,
            Long sellerId,
            Long createdByMemberId
    );

    Optional<ChatRoom> findByIdAndRoomType(Long id, ChatRoomType roomType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select room
            from ChatRoom room
            where room.id = :id
              and room.roomType = :roomType
            """)
    Optional<ChatRoom> findByIdAndRoomTypeWithLock(
            @Param("id") Long id,
            @Param("roomType") ChatRoomType roomType
    );

    boolean existsByRoomTypeAndCreatedByMemberIdAndCsStatusIn(
            ChatRoomType roomType,
            Long createdByMemberId,
            Collection<CsStatus> csStatuses
    );

    @Query("""
            select room
            from ChatRoom room
            where room.roomType = 'PRODUCT'
              and (room.createdByMemberId = :memberId or room.sellerId = :memberId)
            order by room.updatedAt desc
            """)
    List<ChatRoom> findProductRoomsByMemberId(@Param("memberId") Long memberId);

    @Query("""
            select room
            from ChatRoom room
            where room.roomType = 'CS'
              and (:admin = true or room.createdByMemberId = :memberId)
            order by room.updatedAt desc
            """)
    List<ChatRoom> findCsRooms(@Param("memberId") Long memberId, @Param("admin") boolean admin);
}
