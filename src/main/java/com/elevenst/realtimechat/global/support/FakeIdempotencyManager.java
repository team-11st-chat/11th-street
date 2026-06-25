package com.elevenst.realtimechat.global.support;

import org.springframework.stereotype.Component;

@Component
public class FakeIdempotencyManager implements IdempotencyManager {
    // 다른 Issue 구현 완료 후 실제 구현체(Redis)로 교체 예정
    @Override
    public boolean checkAndSet(String requestId, long ttlSeconds) {
        return true; // 무조건 성공 처리
    }
}
