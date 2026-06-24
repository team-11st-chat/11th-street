package com.elevenst.realtimechat.member.service;

import com.elevenst.realtimechat.global.exception.ServiceException;
import com.elevenst.realtimechat.member.dto.MemberCreateRequest;
import com.elevenst.realtimechat.member.dto.MemberResponse;
import com.elevenst.realtimechat.member.entity.Member;
import com.elevenst.realtimechat.member.exception.MemberErrorCode;
import com.elevenst.realtimechat.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public MemberResponse signup(MemberCreateRequest request) {
        if (memberRepository.existsByEmail(request.email())) {
            throw new ServiceException(MemberErrorCode.EMAIL_DUPLICATED);
        }
        String passwordHash = passwordEncoder.encode(request.password());
        Member member = Member.create(request.email(), passwordHash, request.name());
        try {
            return MemberResponse.from(memberRepository.saveAndFlush(member));
        } catch (DataIntegrityViolationException e) {
            throw new ServiceException(MemberErrorCode.EMAIL_DUPLICATED);
        }
    }
}
