package com.elevenst.realtimechat.global.lock;

import com.elevenst.realtimechat.global.support.LockManager;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Slf4j
@Primary
@Component
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
