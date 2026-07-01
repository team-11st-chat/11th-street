package com.elevenst.realtimechat.global.lock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.elevenst.realtimechat.global.support.LockManager;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

@ExtendWith(MockitoExtension.class)
class RedissonLockManagerTest {

    private static final String LOCK_KEY = "lock:timesale:1";

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @AfterEach
    void clearInterruptedStatus() {
        Thread.interrupted();
    }

    @Test
    void tryLockUsesDefaultPolicy() throws InterruptedException {
        RedissonLockManager lockManager = new RedissonLockManager(redissonClient, 3L, 5L);

        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(
                3L,
                5L,
                TimeUnit.SECONDS
        )).thenReturn(true);

        boolean locked = lockManager.tryLock(LOCK_KEY);

        assertThat(locked).isTrue();
        verify(lock).tryLock(3L, 5L, TimeUnit.SECONDS);
    }

    @Test
    void tryLockReturnsFalseWhenRedissonFails() throws InterruptedException {
        RedissonLockManager lockManager = new RedissonLockManager(redissonClient, 3L, 5L);

        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(3L, 5L, TimeUnit.SECONDS)).thenThrow(new IllegalStateException("redis unavailable"));

        boolean locked = lockManager.tryLock(LOCK_KEY);

        assertThat(locked).isFalse();
    }

    @Test
    void tryLockRestoresInterruptedStatusAndReturnsFalse() throws InterruptedException {
        RedissonLockManager lockManager = new RedissonLockManager(redissonClient, 3L, 5L);

        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.tryLock(3L, 5L, TimeUnit.SECONDS)).thenThrow(new InterruptedException("interrupted"));

        boolean locked = lockManager.tryLock(LOCK_KEY);

        assertThat(locked).isFalse();
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }

    @Test
    void unlockReleasesLockOnlyWhenCurrentThreadOwnsIt() {
        RedissonLockManager lockManager = new RedissonLockManager(redissonClient, 3L, 5L);

        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(true);

        lockManager.unlock(LOCK_KEY);

        verify(lock).unlock();
    }

    @Test
    void unlockSkipsWhenCurrentThreadDoesNotOwnLock() {
        RedissonLockManager lockManager = new RedissonLockManager(redissonClient, 3L, 5L);

        when(redissonClient.getLock(LOCK_KEY)).thenReturn(lock);
        when(lock.isHeldByCurrentThread()).thenReturn(false);

        lockManager.unlock(LOCK_KEY);

        verify(lock, never()).unlock();
    }
}
