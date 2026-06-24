package com.elevenst.realtimechat.auth.service;

import com.elevenst.realtimechat.auth.dto.AuthTokens;
import com.elevenst.realtimechat.auth.dto.LoginRequest;
import com.elevenst.realtimechat.auth.exception.AuthErrorCode;
import com.elevenst.realtimechat.global.exception.ServiceException;
import com.elevenst.realtimechat.global.security.TokenProvider;
import com.elevenst.realtimechat.member.entity.Member;
import com.elevenst.realtimechat.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;

    @Transactional(readOnly = true)
    public AuthTokens login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new ServiceException(AuthErrorCode.INVALID_CREDENTIALS));
        if (!member.isActive() || !passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new ServiceException(AuthErrorCode.INVALID_CREDENTIALS);
        }
        String accessToken = tokenProvider.createAccessToken(member.getId(), member.getRole());
        String refreshToken = tokenProvider.createRefreshToken(member.getId());
        return new AuthTokens(accessToken, refreshToken, tokenProvider.getAccessTokenValiditySeconds());
    }
}
