package com.elevenst.realtimechat.global.security;

import com.elevenst.realtimechat.member.entity.MemberRole;

/**
 * 토큰 발급 포트. 이슈 #21 의 "토큰 발급" 흐름이 의존하는 경계.
 * 실제 서명(HS256/JWT)·키 관리는 보안 인프라 작업 단위 소유이며 현재는 {@link FakeTokenProvider} 로 대체되어 있다.
 */
public interface TokenProvider {

    String createAccessToken(Long memberId, MemberRole role);

    String createRefreshToken(Long memberId);

    long getAccessTokenValiditySeconds();

    long getRefreshTokenValiditySeconds();
}
