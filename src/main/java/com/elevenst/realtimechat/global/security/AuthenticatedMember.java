package com.elevenst.realtimechat.global.security;

import com.elevenst.realtimechat.domain.member.entity.MemberRole;

/**
 * 인증 필터가 검증한 Access Token 에서 추출해 SecurityContext 에 보관하는 공통 인증 정보.
 * 컨트롤러는 {@code @AuthenticationPrincipal AuthenticatedMember} 로 회원 ID 와 역할을 받는다.
 */
public record AuthenticatedMember(
        Long memberId,
        MemberRole role
) {
}
