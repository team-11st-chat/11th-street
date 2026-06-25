package com.elevenst.realtimechat.global.support;

public interface IdempotencyManager {
    boolean checkAndSet(String requestId, long ttlSeconds);
}
