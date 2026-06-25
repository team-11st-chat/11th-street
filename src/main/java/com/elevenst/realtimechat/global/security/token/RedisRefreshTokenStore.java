package com.elevenst.realtimechat.global.security.token;

import com.elevenst.realtimechat.global.exception.BusinessException;
import com.elevenst.realtimechat.global.exception.CommonErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Set;
import java.time.Instant;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 기반 {@link RefreshTokenStore} 구현. SHA-256 해시만 저장하고 원문은 보관하지 않는다.
 *
 * <p>키 구조
 * <ul>
 *     <li>{@code auth:refresh:{memberId}:{jti}} → 해시(문자열, TTL = 남은 유효기간)</li>
 *     <li>{@code auth:refresh:index:{memberId}} → 해당 사용자의 활성 jti 집합 (전체 폐기용 색인)</li>
 * </ul>
 *
 * <p>Refresh 흐름은 보안에 민감한 쓰기 흐름이므로 Redis 장애 시 fail-closed 로 동작한다.
 * (정책: Redis 장애 시 재발급/로그아웃 요청은 실패)
 */
@Component
public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String VALUE_KEY_PREFIX = "auth:refresh:";
    private static final String INDEX_KEY_PREFIX = "auth:refresh:index:";
    private static final String GRACE_KEY_PREFIX = "auth:refresh:grace:";

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(Long memberId, String jti, String rawToken, long ttlSeconds) {
        try {
            long now = Instant.now().getEpochSecond();
            long expireAt = now + ttlSeconds;
            Duration ttl = Duration.ofSeconds(ttlSeconds);

            redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                @SuppressWarnings("unchecked")
                public Object execute(RedisOperations operations) throws DataAccessException {
                    RedisOperations<String, String> ops = (RedisOperations<String, String>) operations;
                    ops.opsForValue().set(valueKey(memberId, jti), hash(rawToken), ttl);
                    ops.opsForZSet().add(indexKey(memberId), jti, expireAt);
                    ops.opsForZSet().removeRangeByScore(indexKey(memberId), 0, now);
                    ops.expire(indexKey(memberId), ttl);
                    return null;
                }
            });
        } catch (DataAccessException e) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    @Override
    public boolean matches(Long memberId, String jti, String rawToken) {
        try {
            String stored = redisTemplate.opsForValue().get(valueKey(memberId, jti));
            if (stored == null) {
                return false;
            }
            return MessageDigest.isEqual(
                    stored.getBytes(StandardCharsets.UTF_8),
                    hash(rawToken).getBytes(StandardCharsets.UTF_8));
        } catch (DataAccessException e) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    @Override
    public void delete(Long memberId, String jti) {
        try {
            redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                @SuppressWarnings("unchecked")
                public Object execute(RedisOperations operations) throws DataAccessException {
                    RedisOperations<String, String> ops = (RedisOperations<String, String>) operations;
                    ops.delete(valueKey(memberId, jti));
                    ops.opsForZSet().remove(indexKey(memberId), jti);
                    return null;
                }
            });
        } catch (DataAccessException e) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    @Override
    public void deleteAll(Long memberId) {
        try {
            Set<String> jtis = redisTemplate.opsForZSet().range(indexKey(memberId), 0, -1);
            redisTemplate.executePipelined(new SessionCallback<Object>() {
                @Override
                @SuppressWarnings("unchecked")
                public Object execute(RedisOperations operations) throws DataAccessException {
                    RedisOperations<String, String> ops = (RedisOperations<String, String>) operations;
                    if (jtis != null) {
                        jtis.forEach(jti -> ops.delete(valueKey(memberId, jti)));
                    }
                    ops.delete(indexKey(memberId));
                    return null;
                }
            });
        } catch (DataAccessException e) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    @Override
    public void saveGracePeriodTokens(Long memberId, String oldJti, String accessToken, String refreshToken, long ttlSeconds) {
        try {
            Duration ttl = Duration.ofSeconds(ttlSeconds);
            String value = accessToken + "::" + refreshToken;
            redisTemplate.opsForValue().set(graceKey(memberId, oldJti), value, ttl);
        } catch (DataAccessException e) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    @Override
    public String[] getGracePeriodTokens(Long memberId, String oldJti) {
        try {
            String stored = redisTemplate.opsForValue().get(graceKey(memberId, oldJti));
            if (stored == null) {
                return null;
            }
            String[] parts = stored.split("::");
            if (parts.length == 2) {
                return parts;
            }
            return null;
        } catch (DataAccessException e) {
            throw new BusinessException(CommonErrorCode.SERVICE_UNAVAILABLE, e);
        }
    }

    private String valueKey(Long memberId, String jti) {
        return VALUE_KEY_PREFIX + memberId + ":" + jti;
    }

    private String indexKey(Long memberId) {
        return INDEX_KEY_PREFIX + memberId;
    }

    private String graceKey(Long memberId, String oldJti) {
        return GRACE_KEY_PREFIX + memberId + ":" + oldJti;
    }

    private String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 은 모든 JVM 이 제공하므로 실제로 발생하지 않는다.
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
