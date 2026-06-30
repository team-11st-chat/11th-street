package com.elevenst.realtimechat.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.elevenst.realtimechat.domain.member.entity.MemberRole;
import com.elevenst.realtimechat.global.security.token.AccessTokenBlacklist;
import com.elevenst.realtimechat.global.security.token.TokenClaims;
import com.elevenst.realtimechat.global.security.token.TokenInvalidationRegistry;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;

class JwtTokenValidatorTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256-0123456789";
    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private final JwtTokenProvider provider =
            new JwtTokenProvider(new JwtProperties(SECRET, 3600, 1209600, false));

    private final FakeAccessTokenBlacklist blacklist = new FakeAccessTokenBlacklist();
    private final FakeTokenInvalidationRegistry registry = new FakeTokenInvalidationRegistry();

    private final JwtTokenValidator validator =
            new JwtTokenValidator(provider, blacklist, registry);

    @Test
    void 유효한_액세스_토큰은_검증을_통과하고_클레임을_반환한다() {
        String access = provider.createAccessToken(42L, MemberRole.BUYER).tokenValue();

        TokenClaims claims = validator.validate(access);

        assertThat(claims).isNotNull();
        assertThat(claims.memberId()).isEqualTo(42L);
        assertThat(claims.role()).isEqualTo("BUYER");
    }

    @Test
    void 위조된_토큰은_JwtException이_발생한다() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "another-secret-key-that-is-long-enough-for-hs256-9876543210".getBytes(StandardCharsets.UTF_8));
        String forged = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("42")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .claim("type", "access")
                .claim("role", "BUYER")
                .signWith(otherKey, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> validator.validate(forged))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void 만료된_토큰은_JwtException이_발생한다() {
        Instant past = Instant.now().minusSeconds(7200);
        String expired = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("42")
                .issuedAt(Date.from(past))
                .expiration(Date.from(past.plusSeconds(3600)))
                .claim("type", "access")
                .claim("role", "BUYER")
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> validator.validate(expired))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void 리프레시_토큰은_Access_토큰이_아니므로_JwtException이_발생한다() {
        String refresh = provider.createRefreshToken(42L).tokenValue();

        assertThatThrownBy(() -> validator.validate(refresh))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Token type is not access token");
    }

    @Test
    void Role_클레임이_없는_토큰은_JwtException이_발생한다() {
        String tokenWithoutRole = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("42")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .claim("type", "access")
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> validator.validate(tokenWithoutRole))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Token role claim is missing");
    }

    @Test
    void 블랙리스트에_등록된_토큰은_JwtException이_발생한다() {
        var generated = provider.createAccessToken(42L, MemberRole.BUYER);
        blacklist.add(generated.jti());

        assertThatThrownBy(() -> validator.validate(generated.tokenValue()))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Token is blacklisted");
    }

    @Test
    void 사용자_무효화_기준에_걸린_토큰은_JwtException이_발생한다() {
        registry.invalidateAll(42L);
        String access = provider.createAccessToken(42L, MemberRole.BUYER).tokenValue();

        assertThatThrownBy(() -> validator.validate(access))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Token is invalidated by user session reset");
    }

    @Test
    void 알_수_없는_Role_클레임은_IllegalArgumentException이_발생한다() {
        String unknownRole = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("42")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .claim("type", "access")
                .claim("role", "UNKNOWN_ROLE")
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();

        assertThatThrownBy(() -> validator.validate(unknownRole))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static final class FakeAccessTokenBlacklist implements AccessTokenBlacklist {
        private final Set<String> blacklisted = new HashSet<>();

        void add(String jti) {
            blacklisted.add(jti);
        }

        @Override
        public void blacklist(String jti, long ttlSeconds) {
            blacklisted.add(jti);
        }

        @Override
        public boolean contains(String jti) {
            return blacklisted.contains(jti);
        }
    }

    private static final class FakeTokenInvalidationRegistry implements TokenInvalidationRegistry {
        private final Set<Long> invalidatedMembers = new HashSet<>();

        void invalidateAll(Long memberId) {
            invalidatedMembers.add(memberId);
        }

        @Override
        public void invalidateBefore(Long memberId, Instant instant) {
            invalidatedMembers.add(memberId);
        }

        @Override
        public boolean isInvalidated(Long memberId, Instant tokenIssuedAt) {
            return invalidatedMembers.contains(memberId);
        }
    }
}
