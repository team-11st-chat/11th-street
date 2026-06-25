package com.elevenst.realtimechat.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.elevenst.realtimechat.domain.member.entity.MemberRole;
import com.elevenst.realtimechat.global.security.token.AccessTokenBlacklist;
import com.elevenst.realtimechat.global.security.token.TokenInvalidationRegistry;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256-0123456789";
    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private final JwtTokenProvider provider =
            new JwtTokenProvider(new JwtProperties(SECRET, 3600, 1209600, false));

    private final FakeAccessTokenBlacklist blacklist = new FakeAccessTokenBlacklist();
    private final FakeTokenInvalidationRegistry registry = new FakeTokenInvalidationRegistry();

    private final JwtAuthenticationFilter filter =
            new JwtAuthenticationFilter(provider, blacklist, registry);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 토큰이_없으면_인증을_설정하지_않는다() throws Exception {
        doFilter(new MockHttpServletRequest());

        assertThat(currentAuthentication()).isNull();
    }

    @Test
    void 위조_토큰은_인증을_설정하지_않는다() throws Exception {
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

        doFilter(bearerRequest(forged));

        assertThat(currentAuthentication()).isNull();
    }

    @Test
    void 만료된_토큰은_인증을_설정하지_않는다() throws Exception {
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

        doFilter(bearerRequest(expired));

        assertThat(currentAuthentication()).isNull();
    }

    @Test
    void 리프레시_토큰은_인증을_설정하지_않는다() throws Exception {
        String refresh = provider.createRefreshToken(42L).tokenValue();

        doFilter(bearerRequest(refresh));

        assertThat(currentAuthentication()).isNull();
    }

    @Test
    void 유효한_액세스_토큰은_인증을_설정한다() throws Exception {
        String access = provider.createAccessToken(42L, MemberRole.BUYER).tokenValue();

        doFilter(bearerRequest(access));

        Authentication authentication = currentAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal())
                .isEqualTo(new AuthenticatedMember(42L, MemberRole.BUYER));
        assertThat(authentication.getAuthorities()).extracting("authority").containsExactly("ROLE_BUYER");
    }

    @Test
    void 블랙리스트에_등록된_토큰은_인증을_설정하지_않는다() throws Exception {
        var generated = provider.createAccessToken(42L, MemberRole.BUYER);
        blacklist.add(generated.jti());

        doFilter(bearerRequest(generated.tokenValue()));

        assertThat(currentAuthentication()).isNull();
    }

    @Test
    void 사용자_무효화_기준에_걸린_토큰은_인증을_설정하지_않는다() throws Exception {
        registry.invalidateAll(42L);
        String access = provider.createAccessToken(42L, MemberRole.BUYER).tokenValue();

        doFilter(bearerRequest(access));

        assertThat(currentAuthentication()).isNull();
    }

    @Test
    void 알_수_없는_role_클레임은_500이_아니라_인증_미설정으로_처리된다() throws Exception {
        String unknownRole = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject("42")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .claim("type", "access")
                .claim("role", "UNKNOWN_ROLE")
                .signWith(KEY, Jwts.SIG.HS256)
                .compact();

        MockHttpServletRequest request = bearerRequest(unknownRole);
        assertThatCode(() -> doFilter(request)).doesNotThrowAnyException();
        assertThat(currentAuthentication()).isNull();
    }

    private void doFilter(MockHttpServletRequest request) throws Exception {
        FilterChain chain = (req, res) -> {};
        filter.doFilter(request, new MockHttpServletResponse(), chain);
    }

    private MockHttpServletRequest bearerRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }

    private Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
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
