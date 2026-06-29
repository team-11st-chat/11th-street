package com.elevenst.realtimechat.global.lock;

import com.elevenst.realtimechat.global.support.LockManager;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
// 단일 분산 락 구현체(test 포함 모든 환경의 기본 LockManager). nolock 은 보고서의 Before(락 미적용)
// 측정 전용 프로파일이라 그때만 NoOpLockManager 가 대신 활성화된다. prod 와 nolock 이 함께 켜지면
// 운영 안전을 위해 RedissonLockManager 를 유지한다. 동시성 테스트는 LockManager 를 Mockito 로 대체한다.
@Profile("!nolock | prod")
@RequiredArgsConstructor
public class RedissonLockManager implements LockManager {

    private final RedissonClient redissonClient;

    @Override
    public boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) {
        try {
            RLock lock = redissonClient.getLock(key);
            return lock.tryLock(waitTime, leaseTime, unit);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Redis lock acquisition was interrupted. key={}", key, e);
            return false;
        } catch (RuntimeException e) {
            log.warn("Redis lock acquisition failed. key={}", key, e);
            return false;
        }
    }

    @Override
    public void unlock(String key) {
        try {
            RLock lock = redissonClient.getLock(key);
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                return;
            }

            log.warn("Redis lock unlock skipped because current thread does not own the lock. key={}", key);
        } catch (RuntimeException e) {
            log.warn("Redis lock release failed. key={}", key, e);
        }
    }
}
