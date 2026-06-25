package com.elevenst.realtimechat.global.security.token;

import java.time.Instant;

/**
 * 사용자별 토큰 무효화 기준 시각(token_invalid_before)을 관리한다.
 * 기준 시각 이전에 발급된 Access Token 은 인증을 거부한다.
 *
 * <p>등록 측({@link #invalidateBefore})은 비밀번호 변경·전체 로그아웃 등 사용자 전체 무효화 흐름이 호출하며,
 * 조회 측({@link #isInvalidated})은 인증 필터(이슈 #23)가 호출한다. 인터페이스로 분리해 두어
 * 각 흐름이 구현체에 직접 의존하지 않도록 한다.
 */
public interface TokenInvalidationRegistry {

    /**
     * 해당 시각 이전에 발급된 모든 Access Token 을 무효화한다.
     */
    void invalidateBefore(Long memberId, Instant instant);

    /**
     * 주어진 발급 시각의 토큰이 사용자 무효화 기준에 의해 거부되어야 하는지 조회한다.
     *
     * <p>인증 시 호출되는 조회 경로로, Redis 장애 시에는 fail-open 으로 {@code false} 를 반환한다.
     */
    boolean isInvalidated(Long memberId, Instant tokenIssuedAt);
}
