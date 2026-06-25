package com.elevenst.realtimechat.global.security;

import com.elevenst.realtimechat.domain.member.entity.MemberRole;
import com.elevenst.realtimechat.global.security.token.GeneratedToken;
import com.elevenst.realtimechat.global.security.token.TokenClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenValiditySeconds;
    private final long refreshTokenValiditySeconds;

    public JwtTokenProvider(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenValiditySeconds = properties.accessTokenValiditySeconds();
        this.refreshTokenValiditySeconds = properties.refreshTokenValiditySeconds();
    }

    public GeneratedToken createAccessToken(Long memberId, MemberRole role) {
        String jti = UUID.randomUUID().toString();
        String token = baseBuilder(memberId, jti, accessTokenValiditySeconds)
                .claim("type", "access")
                .claim("role", role.name())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new GeneratedToken(token, jti);
    }

    public GeneratedToken createRefreshToken(Long memberId) {
        String jti = UUID.randomUUID().toString();
        String token = baseBuilder(memberId, jti, refreshTokenValiditySeconds)
                .claim("type", "refresh")
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new GeneratedToken(token, jti);
    }

    public long getAccessTokenValiditySeconds() {
        return accessTokenValiditySeconds;
    }

    public long getRefreshTokenValiditySeconds() {
        return refreshTokenValiditySeconds;
    }

    /**
     * 서명과 만료를 검증하고 핵심 클레임을 추출한다. 검증 실패 시 {@link io.jsonwebtoken.JwtException}
     * (만료 시 {@link io.jsonwebtoken.ExpiredJwtException}) 또는 형식 오류 시
     * {@link IllegalArgumentException} 를 던지므로, 호출하는 인증 흐름에서 예외를 변환한다.
     */
    public TokenClaims parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new TokenClaims(
                Long.valueOf(claims.getSubject()),
                claims.getId(),
                claims.get("type", String.class),
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant());
    }

    private io.jsonwebtoken.JwtBuilder baseBuilder(Long memberId, String jti, long validitySeconds) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(jti)
                .subject(String.valueOf(memberId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(validitySeconds)));
    }
}
