package com.elevenst.realtimechat.domain.auth.controller;

import com.elevenst.realtimechat.domain.auth.dto.AuthTokens;
import com.elevenst.realtimechat.domain.auth.dto.LoginRequest;
import com.elevenst.realtimechat.domain.auth.dto.LoginResponse;
import com.elevenst.realtimechat.domain.auth.service.AuthService;
import com.elevenst.realtimechat.global.response.ApiResponse;
import com.elevenst.realtimechat.global.security.JwtProperties;
import com.elevenst.realtimechat.global.security.JwtTokenProvider;
import jakarta.validation.Valid;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = authService.login(request);
        LoginResponse body = LoginResponse.of(tokens);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken()).toString())
                .body(ApiResponse.success(body));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken) {
        AuthTokens tokens = authService.refresh(refreshToken);
        LoginResponse body = LoginResponse.of(tokens);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken()).toString())
                .body(ApiResponse.success(body));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization) {
        authService.logout(normalizeToken(refreshToken), resolveBearerToken(authorization));
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredRefreshCookie().toString())
                .body(ApiResponse.success());
    }

    private String resolveBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }

        String token = authorization.substring(BEARER_PREFIX.length());
        if (token.isBlank()) {
            return null;
        }

        return token;
    }

    private ResponseCookie expiredRefreshCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    private ResponseCookie refreshCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure())
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofSeconds(jwtTokenProvider.getRefreshTokenValiditySeconds()))
                .build();
    }

    private String normalizeToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return token;
    }
}
