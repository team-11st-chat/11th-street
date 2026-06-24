package com.elevenst.realtimechat.member.dto;

import com.elevenst.realtimechat.member.entity.Member;

public record MemberResponse(Long id, String email, String name) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.getId(), member.getEmail(), member.getName());
    }
}
