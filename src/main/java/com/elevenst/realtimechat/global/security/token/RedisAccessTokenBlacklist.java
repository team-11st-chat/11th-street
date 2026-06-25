package com.elevenst.realtimechat.global.security.token;

import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.exception.CommonErrorCode;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 기반 {@link AccessTokenBlacklist} 구현. 키 {@code auth:blacklist:{jti}} 에 TTL 과 함께 등록한다.
 *
 * <p>등록은 로그아웃(쓰기) 흐름이므로 Redis 장애 시 fail-closed 로 동작하고,
 * 조회는 인증(읽기) 흐름이므로 fail-open 으로 동작한다. (정책: JWT 인증·인가 정책의 Redis 장애 정책)
 */
@Slf4j
@Component
public class RedisAccessTokenBlacklist implements AccessTokenBlacklist {

    private static final String KEY_PREFIX = "auth:blacklist:";
    private static final String BLACKLISTED = "1";

    private final StringRedisTemplate redisTemplate;

    public RedisAccessTokenBlacklist(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void blacklist(String jti, long ttlSeconds) {
        if (ttlSeconds <= 0) {
            // 이미 만료된 토큰은 블랙리스트에 둘 필요가 없다.
            return;
        }
        try {
            redisTemplate.opsForValue().set(key(jti), BLACKLISTED, Duration.ofSeconds(ttlSeconds));
        } catch (DataAccessException e) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    @Override
    public boolean contains(String jti) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key(jti)));
        } catch (DataAccessException e) {
            // fail-open: 장애 동안에는 서명·만료 검증을 통과한 토큰 인증을 유지한다.
            log.warn("Redis 장애로 Access Token 블랙리스트를 조회하지 못했습니다. jti={}", jti, e);
            return false;
        }
    }

    private String key(String jti) {
        return KEY_PREFIX + jti;
    }
}
