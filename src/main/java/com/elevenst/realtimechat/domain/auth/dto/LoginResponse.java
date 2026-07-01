package com.elevenst.realtimechat.domain.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {

    public static LoginResponse of(String accessToken, long expiresIn) {
        return new LoginResponse(accessToken, "Bearer", expiresIn);
    }

    public static LoginResponse of(AuthTokens tokens) {
        return of(tokens.accessToken(), tokens.accessTokenExpiresIn());
    }
}
