package com.elevenst.realtimechat.domain.member.dto;

import com.elevenst.realtimechat.domain.member.entity.Member;

public record MemberResponse(Long id, String email, String name) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getName());
    }
}
