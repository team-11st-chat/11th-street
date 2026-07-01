package com.elevenst.realtimechat.domain.auth.dto;

/**
 * 로그인 시 발급되는 토큰 묶음. accessToken 은 응답 본문, refreshToken 은 쿠키로 전달된다.
 */
public record AuthTokens(String accessToken, String refreshToken, long accessTokenExpiresIn) {
}
