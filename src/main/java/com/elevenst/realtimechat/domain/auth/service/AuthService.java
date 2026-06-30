package com.elevenst.realtimechat.domain.auth.service;

import com.elevenst.realtimechat.domain.auth.dto.AuthTokens;
import com.elevenst.realtimechat.domain.auth.dto.LoginRequest;
import com.elevenst.realtimechat.domain.auth.exception.AuthErrorCode;
import com.elevenst.realtimechat.domain.member.entity.Member;
import com.elevenst.realtimechat.domain.member.entity.MemberRole;
import com.elevenst.realtimechat.domain.member.service.MemberQueryService;
import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.security.JwtTokenProvider;
import com.elevenst.realtimechat.global.security.token.AccessTokenBlacklist;
import com.elevenst.realtimechat.global.security.token.GeneratedToken;
import com.elevenst.realtimechat.global.security.token.RefreshTokenStore;
import com.elevenst.realtimechat.global.security.token.TokenClaims;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberQueryService memberQueryService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final AccessTokenBlacklist accessTokenBlacklist;

    @Transactional(readOnly = true)
    public AuthTokens login(LoginRequest request) {
        Member member = memberQueryService.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));
        if (!member.isActive() || !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        return issueTokens(member.getId(), member.getRole());
    }

    /**
     * Refresh Token 으로 Access Token 과 Refresh Token 을 재발급한다.
     * 기존 Refresh Token 은 즉시 폐기(Rotation)하며, 이미 폐기된 토큰이 다시 제시되면 재사용으로 보고
     * 해당 사용자의 모든 Refresh Token 을 폐기한다.
     */
    public AuthTokens refresh(String refreshToken) {
        TokenClaims claims = parseRefreshToken(refreshToken);
        Long memberId = claims.memberId();
        String jti = claims.jti();

        if (!refreshTokenStore.matches(memberId, jti, refreshToken)) {
            // Check if it's a concurrent retry within Grace Period
            String[] graceTokens = refreshTokenStore.getGracePeriodTokens(memberId, jti);
            if (graceTokens != null) {
                return new AuthTokens(
                        graceTokens[0],
                        graceTokens[1],
                        jwtTokenProvider.getAccessTokenValiditySeconds()
                );
            }

            // 서명·만료를 통과했으나 저장소에 없음 → 이미 Rotation/폐기된 토큰의 재사용으로 판단한다.
            refreshTokenStore.deleteAll(memberId);
            throw new BusinessException(AuthErrorCode.REFRESH_TOKEN_REUSED);
        }

        Member member = memberQueryService.findById(memberId)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));
        if (!member.isActive()) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        refreshTokenStore.delete(memberId, jti);
        AuthTokens newTokens = issueTokens(memberId, member.getRole());

        // 동시성 중복 요청을 방어하기 위해 기존 jti를 키로 하여 새 토큰을 10초간 유예 저장한다.
        refreshTokenStore.saveGracePeriodTokens(memberId, jti, newTokens.accessToken(), newTokens.refreshToken(), 10);

        return newTokens;
    }

    /**
     * 로그아웃한다. 제시된 Refresh Token 을 저장소에서 삭제하고, 현재 Access Token 의 jti 를 블랙리스트에 등록한다.
     * 이미 만료·위조된 토큰은 무효화할 대상이 없으므로 조용히 무시한다(멱등).
     */
    public void logout(String refreshToken, String accessToken) {
        revokeRefreshToken(refreshToken);
        blacklistAccessToken(accessToken);
    }

    private AuthTokens issueTokens(Long memberId, MemberRole role) {
        GeneratedToken generatedAccess = jwtTokenProvider.createAccessToken(memberId, role);
        GeneratedToken generatedRefresh = jwtTokenProvider.createRefreshToken(memberId);

        refreshTokenStore.save(
                memberId,
                generatedRefresh.jti(),
                generatedRefresh.tokenValue(),
                jwtTokenProvider.getRefreshTokenValiditySeconds());

        return new AuthTokens(
                generatedAccess.tokenValue(),
                generatedRefresh.tokenValue(),
                jwtTokenProvider.getAccessTokenValiditySeconds());
    }

    private TokenClaims parseRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        TokenClaims claims;
        try {
            claims = jwtTokenProvider.parse(refreshToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (!claims.isRefresh()) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        return claims;
    }

    private void revokeRefreshToken(String refreshToken) {
        if (hasNoText(refreshToken)) {
            return;
        }
        try {
            TokenClaims claims = jwtTokenProvider.parse(refreshToken);
            if (claims.isRefresh()) {
                refreshTokenStore.delete(claims.memberId(), claims.jti());
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // 만료·위조된 Refresh Token 은 폐기할 대상이 없다.
        }
    }

    private void blacklistAccessToken(String accessToken) {
        if (hasNoText(accessToken)) {
            return;
        }
        try {
            TokenClaims claims = jwtTokenProvider.parse(accessToken);
            long remainingSeconds = Duration.between(Instant.now(), claims.expiresAt()).getSeconds();
            if (claims.isAccess() && remainingSeconds > 0) {
                accessTokenBlacklist.blacklist(claims.jti(), remainingSeconds);
            }
        } catch (JwtException | IllegalArgumentException ignored) {
            // 만료·위조된 Access Token 은 블랙리스트에 등록할 필요가 없다.
        }
    }

    private boolean hasNoText(String token) {
        return token == null || token.isBlank();
    }
}
