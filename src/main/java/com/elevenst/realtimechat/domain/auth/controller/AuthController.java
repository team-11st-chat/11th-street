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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = authService.login(request);
        LoginResponse body = LoginResponse.of(tokens.accessToken(), tokens.accessTokenExpiresIn());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken()).toString())
                .body(ApiResponse.success(body));
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
}
