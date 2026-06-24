package com.elevenst.realtimechat.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.elevenst.realtimechat.auth.dto.LoginRequest;
import com.elevenst.realtimechat.global.security.JwtProperties;
import com.elevenst.realtimechat.global.security.JwtTokenProvider;
import com.elevenst.realtimechat.member.dto.MemberCreateRequest;
import com.elevenst.realtimechat.member.entity.Member;
import com.elevenst.realtimechat.member.entity.MemberRole;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 비밀번호 원문과 토큰 원문이 로그(toString 등)·도메인 데이터에 노출되지 않는지 검증한다.
 * 이슈 #21 의 명시적 요구사항.
 */
class SensitiveDataLeakTest {

    private static final String RAW_PASSWORD = "superSecret123";

    @Test
    void 요청_DTO의_toString은_비밀번호_원문을_노출하지_않는다() {
        // given & when
        String signupText = new MemberCreateRequest("a@example.com", RAW_PASSWORD, "이름").toString();
        String loginText = new LoginRequest("a@example.com", RAW_PASSWORD).toString();

        // then
        assertThat(signupText).doesNotContain(RAW_PASSWORD);
        assertThat(loginText).doesNotContain(RAW_PASSWORD);
    }

    @Test
    void 회원_엔티티의_toString은_비밀번호_해시를_노출하지_않는다() {
        // given
        String hash = new BCryptPasswordEncoder().encode(RAW_PASSWORD);
        Member member = Member.create("a@example.com", hash, "이름");

        // when
        String text = member.toString();

        // then
        assertThat(text).doesNotContain(RAW_PASSWORD);
        assertThat(text).doesNotContain(hash);
    }

    @Test
    void 발급된_토큰_본문에_비밀번호_원문이_포함되지_않는다() {
        // given
        JwtTokenProvider provider = new JwtTokenProvider(
                new JwtProperties("test-secret-key-that-is-long-enough-for-hs256-0123456789", 3600, 1209600, false));

        // when
        String accessToken = provider.createAccessToken(1L, MemberRole.BUYER);
        String refreshToken = provider.createRefreshToken(1L);

        // then
        assertThat(decodeAllSegments(accessToken)).doesNotContain(RAW_PASSWORD);
        assertThat(decodeAllSegments(refreshToken)).doesNotContain(RAW_PASSWORD);
    }

    private static String decodeAllSegments(String token) {
        StringBuilder sb = new StringBuilder();
        for (String part : token.split("\\.")) {
            try {
                sb.append(new String(Base64.getUrlDecoder().decode(part), StandardCharsets.UTF_8));
            } catch (IllegalArgumentException ignored) {
                sb.append(part); // 서명 세그먼트는 base64url 이 아닐 수 있으므로 원문 그대로 비교
            }
        }
        return sb.toString();
    }
}
