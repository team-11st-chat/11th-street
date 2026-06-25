package com.elevenst.realtimechat.global.support;

import java.util.concurrent.TimeUnit;

public interface LockManager {
    boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit);
    void unlock(String key);
}
