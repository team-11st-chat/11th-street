package com.elevenst.realtimechat.domain.auth.service;

import com.elevenst.realtimechat.domain.auth.dto.AuthTokens;
import com.elevenst.realtimechat.domain.auth.dto.LoginRequest;
import com.elevenst.realtimechat.domain.auth.exception.AuthErrorCode;
import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.security.JwtTokenProvider;
import com.elevenst.realtimechat.domain.member.entity.Member;
import com.elevenst.realtimechat.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional(readOnly = true)
    public AuthTokens login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_CREDENTIALS));
        if (!member.isActive() || !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        String accessToken = jwtTokenProvider.createAccessToken(member.getId(), member.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(member.getId());
        return new AuthTokens(accessToken, refreshToken, jwtTokenProvider.getAccessTokenValiditySeconds());
    }
}
