package com.elevenst.realtimechat.domain.member.support;

import org.springframework.stereotype.Component;

@Component
public class FakeMemberStub {
    // 다른 Issue 구현 완료 후 실제 JWT 추출기로 교체 예정
    public Long getMemberId() {
        return 2L; // 구매자 ID
    }
}
