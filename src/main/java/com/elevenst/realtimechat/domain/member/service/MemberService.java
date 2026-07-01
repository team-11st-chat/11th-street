package com.elevenst.realtimechat.domain.member.service;

import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.domain.member.dto.MemberCreateRequest;
import com.elevenst.realtimechat.domain.member.dto.MemberResponse;
import com.elevenst.realtimechat.domain.member.entity.Member;
import com.elevenst.realtimechat.domain.member.exception.MemberErrorCode;
import com.elevenst.realtimechat.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public MemberResponse signup(MemberCreateRequest request) {
        String passwordHash = passwordEncoder.encode(request.password());
        Member member = Member.create(request.email(), passwordHash, request.name());
        try {
            // 동시에 두명 이상의 사람이 같은 이메일로 회원가입 요청을 보냈을때 막기 위해서 메서드 내에서 saveAndFlush
            return MemberResponse.from(memberRepository.saveAndFlush(member));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(MemberErrorCode.EMAIL_DUPLICATED);
        }
    }
}
