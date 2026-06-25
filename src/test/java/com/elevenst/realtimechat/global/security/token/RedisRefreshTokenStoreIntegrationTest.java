package com.elevenst.realtimechat.global.security.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.data.connection.RedissonConnectionFactory;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * {@link RedisRefreshTokenStore} 를 실제 Redis(프로덕션과 동일한 Redisson 커넥션) 위에서 검증하는 통합 테스트.
 *
 * <p>단위 테스트({@code AuthServiceTest})는 {@code RefreshTokenStore} 를 목으로 대체하므로 이 구현체의
 * Redis 동작(파이프라인 명령 지원, 키 자료구조, TTL, 재사용 탐지 근거)을 전혀 검증하지 못한다. 이 테스트가
 * 그 공백을 메운다. 특히 인덱스 키가 정렬셋(ZSET)으로 생성되는지를 못박아, 과거 SET → ZSET 자료구조 변경
 * (커밋 39475f3)으로 발생했던 {@code WRONGTYPE} 회귀를 다시 잡는다.
 *
 * <p>Redis 가 없는 환경에서는 전체 테스트를 건너뛴다(assumeTrue). 로컬에서는 {@code docker compose up} 으로
 * Redis 를 띄운 뒤 실행한다.
 */
class RedisRefreshTokenStoreIntegrationTest {

    private static final Long MEMBER_ID = 987_654_321L;
    private static final long TTL_SECONDS = 60L;

    private static RedissonClient redissonClient;
    private static StringRedisTemplate redisTemplate;
    private static RefreshTokenStore store;

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

            store = new RedisRefreshTokenStore(redisTemplate);
        } catch (Exception e) {
            assumeTrue(false, "Redis 를 사용할 수 없어 통합 테스트를 건너뜁니다: " + e.getMessage());
        }
    }

    @AfterEach
    void cleanUp() {
        if (redisTemplate == null) {
            return;
        }
        redisTemplate.delete(redisTemplate.keys("auth:refresh:" + MEMBER_ID + ":*"));
        redisTemplate.delete("auth:refresh:index:" + MEMBER_ID);
        redisTemplate.delete(redisTemplate.keys("auth:refresh:grace:" + MEMBER_ID + ":*"));
    }

    @AfterAll
    static void disconnect() {
        if (redissonClient != null) {
            redissonClient.shutdown();
        }
    }

    @Test
    @DisplayName("save 한 토큰은 matches 로 일치 확인되고, delete 후에는 더 이상 일치하지 않는다")
    void save_matches_delete_lifecycle() {
        String jti = UUID.randomUUID().toString();
        String rawToken = "raw-refresh-token-" + jti;

        store.save(MEMBER_ID, jti, rawToken, TTL_SECONDS);
        assertThat(store.matches(MEMBER_ID, jti, rawToken)).isTrue();
        assertThat(store.matches(MEMBER_ID, jti, "다른-토큰")).isFalse();

        store.delete(MEMBER_ID, jti);
        // 폐기된 토큰의 재제시는 일치하지 않는다 → 재사용 탐지의 근거.
        assertThat(store.matches(MEMBER_ID, jti, rawToken)).isFalse();
    }

    @Test
    @DisplayName("save 는 사용자 인덱스를 정렬셋(ZSET)으로 생성한다")
    void save_creates_index_as_sorted_set() {
        String jti = UUID.randomUUID().toString();

        store.save(MEMBER_ID, jti, "raw-" + jti, TTL_SECONDS);

        // 과거 SET 기반 인덱스로 회귀하면 ZADD 가 WRONGTYPE 으로 깨진다. 자료구조를 못박아 회귀를 방지한다.
        assertThat(redisTemplate.type("auth:refresh:index:" + MEMBER_ID)).isEqualTo(DataType.ZSET);
    }

    @Test
    @DisplayName("save 한 값 키에는 TTL 이 설정된다")
    void save_sets_ttl_on_value_key() {
        String jti = UUID.randomUUID().toString();

        store.save(MEMBER_ID, jti, "raw-" + jti, TTL_SECONDS);

        Long ttl = redisTemplate.getExpire("auth:refresh:" + MEMBER_ID + ":" + jti);
        assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(TTL_SECONDS);
    }

    @Test
    @DisplayName("deleteAll 은 사용자의 모든 Refresh Token 과 인덱스를 폐기한다")
    void deleteAll_revokes_every_token() {
        String jti1 = UUID.randomUUID().toString();
        String jti2 = UUID.randomUUID().toString();
        store.save(MEMBER_ID, jti1, "raw-" + jti1, TTL_SECONDS);
        store.save(MEMBER_ID, jti2, "raw-" + jti2, TTL_SECONDS);

        store.deleteAll(MEMBER_ID);

        assertThat(store.matches(MEMBER_ID, jti1, "raw-" + jti1)).isFalse();
        assertThat(store.matches(MEMBER_ID, jti2, "raw-" + jti2)).isFalse();
        assertThat(redisTemplate.hasKey("auth:refresh:index:" + MEMBER_ID)).isFalse();
    }

    @Test
    @DisplayName("Grace Period 토큰을 저장하면 동일 키로 다시 조회된다")
    void grace_period_tokens_round_trip() {
        String oldJti = UUID.randomUUID().toString();

        store.saveGracePeriodTokens(MEMBER_ID, oldJti, "new-access", "new-refresh", 10L);

        String[] tokens = store.getGracePeriodTokens(MEMBER_ID, oldJti);
        assertThat(tokens).containsExactly("new-access", "new-refresh");
        assertThat(store.getGracePeriodTokens(MEMBER_ID, UUID.randomUUID().toString())).isNull();
    }
}
