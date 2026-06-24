package com.elevenst.realtimechat.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresIn) {

    public static LoginResponse of(String accessToken, long expiresIn) {
        return new LoginResponse(accessToken, "Bearer", expiresIn);
    }
}
