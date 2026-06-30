package com.elevenst.realtimechat.global.security;

import com.elevenst.realtimechat.domain.member.entity.MemberRole;
import com.elevenst.realtimechat.global.security.token.TokenClaims;

/**
 * JwtTokenValidator를 통해 검증이 완료된 토큰 정보와 변환 완료된 MemberRole을 담는 객체.
 */
public record ValidatedToken(
        TokenClaims claims,
        MemberRole role
) {
    public Long getMemberId() {
        return claims.memberId();
    }
}
