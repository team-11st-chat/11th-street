package com.elevenst.realtimechat.global.security;

import com.elevenst.realtimechat.domain.member.entity.MemberRole;
import com.elevenst.realtimechat.global.security.token.AccessTokenBlacklist;
import com.elevenst.realtimechat.global.security.token.TokenClaims;
import com.elevenst.realtimechat.global.security.token.TokenInvalidationRegistry;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * JWT 토큰 파싱 및 비즈니스 유효성 검증 정책을 담당하는 클래스.
 */
@Component
public class JwtTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final AccessTokenBlacklist accessTokenBlacklist;
    private final TokenInvalidationRegistry tokenInvalidationRegistry;

    public JwtTokenValidator(
            JwtTokenProvider jwtTokenProvider,
            AccessTokenBlacklist accessTokenBlacklist,
            TokenInvalidationRegistry tokenInvalidationRegistry) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.accessTokenBlacklist = accessTokenBlacklist;
        this.tokenInvalidationRegistry = tokenInvalidationRegistry;
    }

    /**
     * 토큰의 서명, 만료, 위조 검증 및 비즈니스 유효성 정책 검증을 수행하고,
     * 유효한 경우 {@link TokenClaims}를 반환합니다.
     *
     * @param token Bearer 접두사가 제거된 JWT Access Token
     * @return 검증된 토큰의 클레임 정보
     * @throws JwtException JWT 파싱 실패, 타입 오류, 블랙리스트/만료 기준 위반 시 발생
     * @throws IllegalArgumentException 매개변수가 비어있거나 올바르지 않은 경우 발생
     */
    public TokenClaims validate(String token) {
        TokenClaims claims;
        try {
            claims = jwtTokenProvider.parse(token);
        } catch (JwtException e) {
            log.warn("JWT parsing or validation failed: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid JWT argument: {}", e.getMessage());
            throw e;
        }

        if (!claims.isAccess()) {
            log.warn("Invalid JWT type: Expected access token, but got refresh token (memberId: {})", claims.memberId());
            throw new JwtException("Token type is not access token");
        }

        if (claims.role() == null) {
            log.warn("Token role claim is missing (memberId: {})", claims.memberId());
            throw new JwtException("Token role claim is missing");
        }

        if (accessTokenBlacklist.contains(claims.jti())) {
            log.warn("Token is blacklisted (jti: {}, memberId: {})", claims.jti(), claims.memberId());
            throw new JwtException("Token is blacklisted");
        }

        if (tokenInvalidationRegistry.isInvalidated(claims.memberId(), claims.issuedAt())) {
            log.warn("Token is invalidated for member (memberId: {}, issuedAt: {})", claims.memberId(), claims.issuedAt());
            throw new JwtException("Token is invalidated by user session reset");
        }

        try {
            MemberRole.valueOf(claims.role());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown member role in token: {} (memberId: {})", claims.role(), claims.memberId());
            throw e;
        }

        return claims;
    }
}
