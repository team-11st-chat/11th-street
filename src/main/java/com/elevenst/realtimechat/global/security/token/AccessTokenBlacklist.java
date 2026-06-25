package com.elevenst.realtimechat.global.security.token;

/**
 * 로그아웃된 Access Token 의 jti 를 블랙리스트로 관리한다.
 *
 * <p>인터페이스로 분리해 두어 인증 필터(이슈 #23)가 조회 측({@link #contains})에 의존하고,
 * 로그아웃 흐름이 등록 측({@link #blacklist})에 의존하도록 한다.
 */
public interface AccessTokenBlacklist {

    /**
     * Access Token 의 jti 를 블랙리스트에 등록한다. TTL 은 해당 토큰의 남은 유효기간으로 설정한다.
     */
    void blacklist(String jti, long ttlSeconds);

    /**
     * 해당 jti 가 블랙리스트에 있는지 조회한다.
     *
     * <p>인증 시 호출되는 조회 경로로, Redis 장애 시에는 fail-open 으로 {@code false} 를 반환한다.
     * (정책: Redis 장애로 블랙리스트를 조회할 수 없는 동안에는 서명·만료 검증을 통과한 토큰 인증을 유지)
     */
    boolean contains(String jti);
}
