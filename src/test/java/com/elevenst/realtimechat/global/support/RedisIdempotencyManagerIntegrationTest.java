package com.elevenst.realtimechat.global.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * {@link RedisIdempotencyManager} 를 실제 Redis(프로덕션과 동일한 Redisson 커넥션) 위에서 검증하는 통합 테스트.
 *
 * <p>단위 테스트는 {@code IdempotencyManager} 를 목/Fake 로 대체하므로 이 구현체의 Redis 동작(SET NX, TTL,
 * 중복 차단)을 검증하지 못한다. 이 테스트가 그 공백을 메운다.
 *
 * <p>Redis 가 없는 환경에서는 전체 테스트를 건너뛴다(assumeTrue). 로컬에서는 {@code docker compose up} 으로
 * Redis 를 띄운 뒤 실행한다.
 */
@Tag("integration")
class RedisIdempotencyManagerIntegrationTest {

    private static final String KEY_PREFIX = "idempotency:request-id:";
    private static final long TTL_SECONDS = 60L;

    private static RedissonClient redissonClient;
    private static StringRedisTemplate redisTemplate;
    private static RedisIdempotencyManager idempotencyManager;

    @BeforeAll
    static void connect() {
        String host = System.getenv().getOrDefault("REDIS_HOST", "localhost");
        String port = System.getenv().getOrDefault("REDIS_PORT", "6379");

        try {
            Config config = new Config();
            config.useSingleServer().setAddress("redis://" + host + ":" + port);
            redissonClient = Redisson.create(config);

            RedissonConnectionFactory connectionFactory = new RedissonConnectionFactory(redissonClient);
            redisTemplate = new StringRedisTemplate(connectionFactory);
            redisTemplate.afterPropertiesSet();

            idempotencyManager = new RedisIdempotencyManager(redisTemplate);
        } catch (Exception e) {
            assumeTrue(false, "Redis 를 사용할 수 없어 통합 테스트를 건너뜁니다: " + e.getMessage());
        }
    }

    @AfterEach
    void cleanUp() {
        if (redisTemplate != null) {
            redisTemplate.delete(redisTemplate.keys(KEY_PREFIX + "itest-*"));
        }
    }

    @AfterAll
    static void disconnect() {
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }

    @Test
    @DisplayName("최초 Request-ID 는 성공(true), 동일 Request-ID 재시도는 중복으로 차단(false)된다")
    void firstRequestSucceeds_duplicateIsBlocked() {
        String requestId = "itest-" + UUID.randomUUID();

        assertThat(idempotencyManager.checkAndSet(requestId, TTL_SECONDS)).isTrue();
        assertThat(idempotencyManager.checkAndSet(requestId, TTL_SECONDS)).isFalse();
    }

    @Test
    @DisplayName("서로 다른 Request-ID 는 각각 최초 요청으로 성공(true)한다")
    void differentRequestIdsSucceedIndependently() {
        assertThat(idempotencyManager.checkAndSet("itest-" + UUID.randomUUID(), TTL_SECONDS)).isTrue();
        assertThat(idempotencyManager.checkAndSet("itest-" + UUID.randomUUID(), TTL_SECONDS)).isTrue();
    }

    @Test
    @DisplayName("선점한 Request-ID 키에는 TTL 이 설정된다")
    void setsTtlOnKey() {
        String requestId = "itest-" + UUID.randomUUID();

        idempotencyManager.checkAndSet(requestId, TTL_SECONDS);

        Long ttl = redisTemplate.getExpire(KEY_PREFIX + requestId);
        assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(TTL_SECONDS);
    }

    @Test
    @DisplayName("TTL 이 만료되면 같은 Request-ID 가 다시 최초 요청으로 처리된다(재시도 윈도우)")
    void expiredKeyAllowsNewRequest() throws InterruptedException {
        String requestId = "itest-" + UUID.randomUUID();

        assertThat(idempotencyManager.checkAndSet(requestId, 1L)).isTrue();
        assertThat(idempotencyManager.checkAndSet(requestId, 1L)).isFalse();

        Thread.sleep(1_300L);

        assertThat(idempotencyManager.checkAndSet(requestId, 1L)).isTrue();
    }
}
