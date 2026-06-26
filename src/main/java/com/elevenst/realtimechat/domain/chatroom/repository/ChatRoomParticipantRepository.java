package com.elevenst.realtimechat.domain.chatroom.repository;

import com.elevenst.realtimechat.domain.chatroom.entity.ChatRoomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomParticipantRepository extends JpaRepository<ChatRoomParticipant, Long> {

    boolean existsByChatRoomIdAndMemberIdAndLeftAtIsNull(Long chatRoomId, Long memberId);
}
