package com.elevenst.realtimechat.domain.member.controller;

import com.elevenst.realtimechat.global.response.ApiResponse;
import com.elevenst.realtimechat.domain.member.dto.MemberCreateRequest;
import com.elevenst.realtimechat.domain.member.dto.MemberResponse;
import com.elevenst.realtimechat.domain.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberController {

    private final MemberService memberService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MemberResponse> signup(@Valid @RequestBody MemberCreateRequest request) {
        return ApiResponse.success(memberService.signup(request));
    }
}
