package com.elevenst.realtimechat.domain.member.dto;

import com.elevenst.realtimechat.domain.member.entity.Member;
import com.elevenst.realtimechat.domain.member.entity.MemberRole;

public record MemberResponse(Long id, String email, String name, MemberRole role) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getName(), member.getRole());
    }
}
