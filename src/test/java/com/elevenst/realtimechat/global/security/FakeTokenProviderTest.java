package com.elevenst.realtimechat.global.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.elevenst.realtimechat.member.entity.MemberRole;
import org.junit.jupiter.api.Test;

class FakeTokenProviderTest {

    private final FakeTokenProvider provider =
            new FakeTokenProvider(new JwtProperties("unused-secret", 3600, 1209600, false));

    @Test
    void 액세스_토큰은_회원ID와_역할을_담고_매번_다른_값을_발급한다() {
        // when
        String first = provider.createAccessToken(42L, MemberRole.BUYER);
        String second = provider.createAccessToken(42L, MemberRole.BUYER);

        // then
        assertThat(first).contains("42").contains("BUYER");
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void 리프레시_토큰은_액세스_토큰과_구분되고_역할을_담지_않는다() {
        // when
        String refreshToken = provider.createRefreshToken(42L);

        // then
        assertThat(refreshToken).contains("refresh").contains("42");
        assertThat(refreshToken).doesNotContain("BUYER");
    }

    @Test
    void 유효기간_설정값을_그대로_노출한다() {
        // then
        assertThat(provider.getAccessTokenValiditySeconds()).isEqualTo(3600);
        assertThat(provider.getRefreshTokenValiditySeconds()).isEqualTo(1209600);
    }
}
