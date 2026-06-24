package com.elevenst.realtimechat.global.security;

import com.elevenst.realtimechat.member.entity.MemberRole;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * FAKE: 이슈 #21 범위 밖인 "실제 JWT(HS256) 서명·키 관리"를 대체하는 가짜 토큰 발급기.
 * 토큰 발급 흐름(로그인 시 access/refresh 토큰을 만들어 반환)만 유지하기 위한 구현이며,
 * 서명되지 않은 식별용 placeholder 문자열을 반환한다. 비밀번호 원문은 토큰에 포함하지 않는다.
 *
 * TODO(실제 구현 교체 지점): HS256 서명 + 환경/SSM 기반 비밀키로 JWT 를 발급하는 구현체로 교체.
 *   - 유효기간/클레임(sub=memberId, role)은 본 fake 의 계약을 그대로 따른다.
 *   - 교체 시 build.gradle 의 jjwt 의존성을 다시 추가한다.
 */
@Component
@RequiredArgsConstructor
public class FakeTokenProvider implements TokenProvider {

    private final JwtProperties properties;

    @Override
    public String createAccessToken(Long memberId, MemberRole role) {
        return "fake-access-token." + memberId + "." + role + "." + UUID.randomUUID();
    }

    @Override
    public String createRefreshToken(Long memberId) {
        return "fake-refresh-token." + memberId + "." + UUID.randomUUID();
    }

    @Override
    public long getAccessTokenValiditySeconds() {
        return properties.accessTokenValiditySeconds();
    }

    @Override
    public long getRefreshTokenValiditySeconds() {
        return properties.refreshTokenValiditySeconds();
    }
}
