package com.elevenst.realtimechat.global.security.token;

import java.time.Instant;

/**
 * 검증을 통과한 JWT 에서 추출한 핵심 클레임 묶음.
 * jti 는 Refresh Token 저장과 Access Token 블랙리스트의 키로, issuedAt 은 사용자별 무효화 기준 비교에 사용된다.
 * role 은 Access Token 에만 담기며 Refresh Token 에서는 {@code null} 이다.
 */
public record TokenClaims(
        Long memberId,
        String jti,
        String type,
        String role,
        Instant issuedAt,
        Instant expiresAt
) {

    public boolean isAccess() {
        return "access".equals(type);
    }

    public boolean isRefresh() {
        return "refresh".equals(type);
    }
}
