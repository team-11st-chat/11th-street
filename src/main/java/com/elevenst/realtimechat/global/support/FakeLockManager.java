package com.elevenst.realtimechat.global.support;

import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class FakeLockManager implements LockManager {
    // 다른 Issue 구현 완료 후 실제 구현체(Redisson 기반 LockManager)로 교체 예정
    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        return true;
    }

    @Override
    public void unlock(String key) {
        // do nothing
    }
}
