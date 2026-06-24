package com.elevenst.realtimechat.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.elevenst.realtimechat.member.entity.MemberRole;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256-0123456789";

    private final JwtTokenProvider provider =
            new JwtTokenProvider(new JwtProperties(SECRET, 3600, 1209600, false));

    private static String decodePayload(String token) {
        String[] parts = token.split("\\.");
        return new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
    }

    @Test
    void 액세스_토큰은_회원ID와_역할을_담는다() {
        // when
        String token = provider.createAccessToken(42L, MemberRole.BUYER);

        // then
        String payload = decodePayload(token);
        assertThat(payload).contains("\"sub\":\"42\"");
        assertThat(payload).contains("\"role\":\"BUYER\"");
        assertThat(payload).contains("\"type\":\"access\"");
    }

    @Test
    void 리프레시_토큰은_역할을_담지_않는다() {
        // when
        String token = provider.createRefreshToken(42L);

        // then
        String payload = decodePayload(token);
        assertThat(payload).contains("\"sub\":\"42\"");
        assertThat(payload).contains("\"type\":\"refresh\"");
        assertThat(payload).doesNotContain("role");
    }
}
