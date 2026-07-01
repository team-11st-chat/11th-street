package com.elevenst.realtimechat.global.security.token;

import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.exception.CommonErrorCode;
import com.elevenst.realtimechat.global.security.JwtProperties;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 기반 {@link TokenInvalidationRegistry} 구현. 키 {@code auth:invalid-before:{memberId}} 에
 * 무효화 기준 시각(epoch second)을 저장한다.
 *
 * <p>기준 시각 이후로는 Access Token 의 최대 수명(=Access Token 유효기간)이 지나면 그 이전 발급 토큰이
 * 모두 만료되므로, TTL 을 Access Token 유효기간으로 설정해 불필요한 키를 자동 정리한다.
 */
@Slf4j
@Component
public class RedisTokenInvalidationRegistry implements TokenInvalidationRegistry {

    private static final String KEY_PREFIX = "auth:invalid-before:";

    private final StringRedisTemplate redisTemplate;
    private final long accessTokenValiditySeconds;

    public RedisTokenInvalidationRegistry(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.accessTokenValiditySeconds = jwtProperties.accessTokenValiditySeconds();
    }

    @Override
    public void invalidateBefore(Long memberId, Instant instant) {
        try {
            redisTemplate.opsForValue().set(
                    key(memberId),
                    String.valueOf(instant.getEpochSecond()),
                    Duration.ofSeconds(accessTokenValiditySeconds));
        } catch (DataAccessException e) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    @Override
    public boolean isInvalidated(Long memberId, Instant tokenIssuedAt) {
        try {
            String value = redisTemplate.opsForValue().get(key(memberId));
            if (value == null) {
                return false;
            }
            Instant invalidBefore = Instant.ofEpochSecond(Long.parseLong(value));
            return tokenIssuedAt.isBefore(invalidBefore);
        } catch (DataAccessException e) {
            // fail-open: 장애 동안에는 서명·만료 검증을 통과한 토큰 인증을 유지한다.
            log.warn("Redis 장애로 토큰 무효화 기준 시각을 조회하지 못했습니다. memberId={}", memberId, e);
            return false;
        }
    }

    private String key(Long memberId) {
        return KEY_PREFIX + memberId;
    }
}
