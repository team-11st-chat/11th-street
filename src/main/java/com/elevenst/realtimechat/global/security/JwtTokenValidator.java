package com.elevenst.realtimechat.global.security;

import com.elevenst.realtimechat.domain.member.entity.MemberRole;
import com.elevenst.realtimechat.global.security.token.AccessTokenBlacklist;
import com.elevenst.realtimechat.global.security.token.TokenClaims;
import com.elevenst.realtimechat.global.security.token.TokenInvalidationRegistry;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
     * 유효한 경우 {@link ValidatedToken}을 반환합니다.
     *
     * @param token Bearer 접두사가 제거된 JWT Access Token
     * @return 검증된 토큰의 클레임 정보와 매핑된 권한 정보
     * @throws JwtException JWT 파싱 실패, 타입 오류, 블랙리스트/만료 기준 위반 시 발생
     * @throws IllegalArgumentException 매개변수가 비어있거나 올바르지 않은 경우 발생
     */
    public ValidatedToken validate(String token) {
        TokenClaims claims;
        try {
            claims = jwtTokenProvider.parse(token);
        } catch (JwtException e) {
            log.debug("JWT parsing or validation failed: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.debug("Invalid JWT argument: {}", e.getMessage());
            throw e;
        }

        if (!claims.isAccess()) {
            log.debug("Invalid JWT type: Expected access token, but got refresh token (memberId: {})", claims.memberId());
            throw new JwtException("Token type is not access token");
        }

        if (claims.role() == null) {
            log.debug("Token role claim is missing (memberId: {})", claims.memberId());
            throw new JwtException("Token role claim is missing");
        }

        if (!StringUtils.hasText(claims.jti())) {
            log.debug("Token jti claim is missing or empty (memberId: {})", claims.memberId());
            throw new JwtException("Token jti claim is missing or empty");
        }

        if (accessTokenBlacklist.contains(claims.jti())) {
            log.warn("Token is blacklisted (jti: {}, memberId: {})", claims.jti(), claims.memberId());
            throw new JwtException("Token is blacklisted");
        }

        if (tokenInvalidationRegistry.isInvalidated(claims.memberId(), claims.issuedAt())) {
            log.warn("Token is invalidated for member (memberId: {}, issuedAt: {})", claims.memberId(), claims.issuedAt());
            throw new JwtException("Token is invalidated by user session reset");
        }

        MemberRole role;
        try {
            role = MemberRole.valueOf(claims.role());
        } catch (IllegalArgumentException e) {
            log.debug("Unknown member role in token: {} (memberId: {})", claims.role(), claims.memberId());
            throw e;
        }

        return new ValidatedToken(claims, role);
    }
}

