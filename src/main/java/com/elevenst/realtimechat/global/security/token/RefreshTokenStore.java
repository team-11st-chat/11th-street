package com.elevenst.realtimechat.global.security.token;

/**
 * Refresh Token 의 해시를 사용자 ID 와 jti 기준으로 보관하는 저장소.
 * 원문은 저장하지 않으며, Rotation 과 재사용 탐지의 상태 근거가 된다.
 *
 * <p>인터페이스로 분리해 두어 HTTP 인증 필터(이슈 #23)와 테스트가 구현체에 직접 의존하지 않도록 한다.
 */
public interface RefreshTokenStore {

    /**
     * Refresh Token 해시를 저장한다. TTL 은 토큰의 남은 유효기간과 동일하게 설정한다.
     */
    void save(Long memberId, String jti, String rawToken, long ttlSeconds);

    /**
     * 제시된 Refresh Token 이 저장된 해시와 일치하는지 확인한다.
     * 키가 존재하지 않으면(이미 Rotation/폐기됨) {@code false} 를 반환하며, 이는 재사용 탐지의 근거가 된다.
     */
    boolean matches(Long memberId, String jti, String rawToken);

    /**
     * 단일 Refresh Token 을 폐기한다. (Rotation 시 기존 토큰 폐기, 로그아웃 시 현재 토큰 폐기)
     */
    void delete(Long memberId, String jti);

    /**
     * 사용자의 모든 Refresh Token 을 폐기한다. (재사용 탐지 시 보안 조치)
     */
    void deleteAll(Long memberId);
}
