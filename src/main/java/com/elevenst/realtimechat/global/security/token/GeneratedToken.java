package com.elevenst.realtimechat.global.security.token;

/**
 * 토큰 발급 시 문자열 원본과 내부적으로 생성된 jti(식별자)를 함께 반환하기 위한 레코드.
 * 이를 통해 토큰 발급 직후 jti 조회를 위한 불필요한 재파싱을 방지한다.
 */
public record GeneratedToken(String tokenValue, String jti) {
}
