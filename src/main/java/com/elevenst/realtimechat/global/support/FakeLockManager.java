package com.elevenst.realtimechat.global.support;

import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class FakeLockManager implements LockManager {
    // 다른 Issue 구현 완료 후 테스트/로컬 대체가 필요 없으면 실제 구현체(RedissonLockManager)로 완전히 교체 예정
    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        return true;
    }

    @Override
    public void unlock(String key) {
        // do nothing
    }
}
