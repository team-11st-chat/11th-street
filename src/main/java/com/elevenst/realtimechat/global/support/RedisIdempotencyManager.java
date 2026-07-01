package com.elevenst.realtimechat.global.support;

import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.exception.CommonErrorCode;
import java.time.Duration;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 기반 {@link IdempotencyManager} 구현(단일 구현체).
 *
 * <p>Policies 「Request ID 기반 동일 요청 재시도 및 멱등성 처리 정책」을 구현한다. {@code SET key value NX EX ttl}
 * (Spring Data Redis {@code setIfAbsent})로 Request-ID를 원자적으로 선점한다. 최초 요청이면 키를 등록하고
 * {@code true}(진행)를 반환하고, 이미 등록된 Request-ID면 키가 존재해 {@code false}(중복)를 반환한다. 키는
 * 완료/실패 상태 전환 없이 TTL 동안 유지되므로, 처리 중이거나 실패한 동일 Request-ID도 정책상 중복 요청으로 차단된다.
 *
 * <p>Redis 장애로 멱등성을 보장할 수 없으면 정합성을 위해 Fail-Closed로 동작한다(Policies: Redis 장애 시 즉시 거절).
 * 모든 프로파일에서 사용하는 유일한 구현체이며, 단위 테스트는 Mockito로 {@link IdempotencyManager}를 대체한다.
 */
@Component
public class RedisIdempotencyManager implements IdempotencyManager {

    private static final String KEY_PREFIX = "idempotency:request-id:";
    private static final String PROCESSING_VALUE = "PROCESSING";

    private final StringRedisTemplate redisTemplate;

    public RedisIdempotencyManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean checkAndSet(String requestId, long ttlSeconds) {
        try {
            Boolean firstRequest = redisTemplate.opsForValue()
                    .setIfAbsent(KEY_PREFIX + requestId, PROCESSING_VALUE, Duration.ofSeconds(ttlSeconds));
            return Boolean.TRUE.equals(firstRequest);
        } catch (DataAccessException e) {
            // 멱등성 검증이 불가하면 중복/초과 처리를 막기 위해 요청을 거절한다(Fail-Closed).
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }
}
