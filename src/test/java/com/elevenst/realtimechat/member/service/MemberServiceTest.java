package com.elevenst.realtimechat.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.elevenst.realtimechat.domain.member.service.MemberService;
import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.domain.member.dto.MemberCreateRequest;
import com.elevenst.realtimechat.domain.member.dto.MemberResponse;
import com.elevenst.realtimechat.domain.member.entity.Member;
import com.elevenst.realtimechat.domain.member.entity.MemberRole;
import com.elevenst.realtimechat.domain.member.entity.MemberStatus;
import com.elevenst.realtimechat.domain.member.exception.MemberErrorCode;
import com.elevenst.realtimechat.domain.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class MemberServiceTest {

    private MemberRepository memberRepository;
    private PasswordEncoder passwordEncoder;
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberRepository = Mockito.mock(MemberRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        memberService = new MemberService(memberRepository, passwordEncoder);
    }

    @Test
    void 회원가입하면_비밀번호는_해시로_저장되고_역할과_상태는_기본값이다() {
        // given
        MemberCreateRequest request = new MemberCreateRequest("buyer@example.com", "plainPassword1", "구매자");
        given(memberRepository.existsByEmail("buyer@example.com")).willReturn(false);
        given(memberRepository.saveAndFlush(any(Member.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        MemberResponse response = memberService.signup(request);

        // then
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        Mockito.verify(memberRepository).saveAndFlush(captor.capture());
        Member saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isNotEqualTo("plainPassword1");
        assertThat(passwordEncoder.matches("plainPassword1", saved.getPasswordHash())).isTrue();
        assertThat(saved.getRole()).isEqualTo(MemberRole.BUYER);
        assertThat(saved.getStatus()).isEqualTo(MemberStatus.ACTIVE);
        assertThat(response.email()).isEqualTo("buyer@example.com");
    }

    @Test
    void 이미_가입된_이메일이면_예외가_발생한다() {
        // given
        MemberCreateRequest request = new MemberCreateRequest("dup@example.com", "plainPassword1", "중복");
        given(memberRepository.existsByEmail("dup@example.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(MemberErrorCode.EMAIL_DUPLICATED.message());
    }

    @Test
    void 저장시_데이터_정합성_예외가_발생하면_이메일_중복_예외가_발생한다() {
        // given
        MemberCreateRequest request = new MemberCreateRequest("dup@example.com", "plainPassword1", "중복");
        given(memberRepository.existsByEmail("dup@example.com")).willReturn(false);
        given(memberRepository.saveAndFlush(any(Member.class)))
                .willThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate entry"));

        // when & then
        assertThatThrownBy(() -> memberService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(MemberErrorCode.EMAIL_DUPLICATED.message());
    }
}
