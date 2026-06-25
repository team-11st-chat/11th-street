package com.elevenst.realtimechat.global.support;

import java.util.concurrent.TimeUnit;

public interface LockManager {
    long DEFAULT_WAIT_TIME = 3L;
    long DEFAULT_LEASE_TIME = 2L;
    TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;

    default boolean tryLock(String key) {
        return tryLock(key, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, DEFAULT_TIME_UNIT);
    }

    boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit);

    void unlock(String key);
}
