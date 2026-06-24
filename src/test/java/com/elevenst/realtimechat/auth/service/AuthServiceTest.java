package com.elevenst.realtimechat.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.elevenst.realtimechat.domain.auth.dto.AuthTokens;
import com.elevenst.realtimechat.domain.auth.dto.LoginRequest;
import com.elevenst.realtimechat.domain.auth.exception.AuthErrorCode;
import com.elevenst.realtimechat.domain.auth.service.AuthService;
import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.security.JwtProperties;
import com.elevenst.realtimechat.global.security.JwtTokenProvider;
import com.elevenst.realtimechat.domain.member.entity.Member;
import com.elevenst.realtimechat.domain.member.entity.MemberStatus;
import com.elevenst.realtimechat.domain.member.repository.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class AuthServiceTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256-0123456789";

    private MemberRepository memberRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        memberRepository = Mockito.mock(MemberRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        JwtTokenProvider tokenProvider = new JwtTokenProvider(new JwtProperties(SECRET, 3600, 1209600, false));
        authService = new AuthService(memberRepository, passwordEncoder, tokenProvider);
    }

    private Member activeMemberWithPassword(String rawPassword) {
        Member member = Member.create("buyer@example.com", passwordEncoder.encode(rawPassword), "구매자");
        ReflectionTestUtils.setField(member, "id", 1L);
        return member;
    }

    @Test
    void 로그인에_성공하면_액세스_토큰과_리프레시_토큰을_발급한다() {
        // given
        given(memberRepository.findByEmail("buyer@example.com"))
                .willReturn(Optional.of(activeMemberWithPassword("plainPassword1")));

        // when
        AuthTokens tokens = authService.login(new LoginRequest("buyer@example.com", "plainPassword1"));

        // then
        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.accessTokenExpiresIn()).isEqualTo(3600);
    }

    @Test
    void 비밀번호가_일치하지_않으면_예외가_발생한다() {
        // given
        given(memberRepository.findByEmail("buyer@example.com"))
                .willReturn(Optional.of(activeMemberWithPassword("plainPassword1")));

        // when & then
        assertThatThrownBy(() -> authService.login(new LoginRequest("buyer@example.com", "wrongPassword")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(AuthErrorCode.INVALID_CREDENTIALS.message());
    }

    @Test
    void 존재하지_않는_이메일이면_예외가_발생한다() {
        // given
        given(memberRepository.findByEmail("none@example.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(new LoginRequest("none@example.com", "plainPassword1")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(AuthErrorCode.INVALID_CREDENTIALS.message());
    }

    @Test
    void 정지된_계정이면_예외가_발생한다() {
        // given
        Member suspended = activeMemberWithPassword("plainPassword1");
        ReflectionTestUtils.setField(suspended, "status", MemberStatus.SUSPENDED);
        given(memberRepository.findByEmail("buyer@example.com")).willReturn(Optional.of(suspended));

        // when & then
        assertThatThrownBy(() -> authService.login(new LoginRequest("buyer@example.com", "plainPassword1")))
                .isInstanceOf(BusinessException.class)
                .hasMessage(AuthErrorCode.INVALID_CREDENTIALS.message());
    }
}
