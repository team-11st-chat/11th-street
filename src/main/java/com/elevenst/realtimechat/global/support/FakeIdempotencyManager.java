package com.elevenst.realtimechat.global.support;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 테스트 전용 No-Op {@link IdempotencyManager}. 운영 경로는 {@link RedisIdempotencyManager}를 사용한다.
 * ({@code FakeLockManager}/{@code RedissonLockManager}와 동일한 프로파일 분리 방식)
 */
@Component
@Profile("test")
public class FakeIdempotencyManager implements IdempotencyManager {
    @Override
    public boolean checkAndSet(String requestId, long ttlSeconds) {
        return true; // 무조건 성공 처리
    }
}
