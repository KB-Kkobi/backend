package org.kkobi.security.token;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Collections;

@Repository
@RequiredArgsConstructor
public class RedisRefreshTokenStore implements RefreshTokenStore {
    private static final String KEY_PREFIX = "auth:refresh:";
    private static final DefaultRedisScript<Long> CONSUME_SCRIPT = new DefaultRedisScript<>(
            "local value = redis.call('GET', KEYS[1]); "
                    + "if not value or value ~= ARGV[1] then return 0 end; "
                    + "return redis.call('DEL', KEYS[1]);",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String tokenId, String tokenHash, Duration timeToLive) {
        if (timeToLive == null || timeToLive.isZero() || timeToLive.isNegative()) {
            throw new IllegalArgumentException("Refresh Token TTL must be greater than zero");
        }

        redisTemplate.opsForValue().set(key(tokenId), tokenHash, timeToLive);
    }

    // 비교와 삭제를 Lua 스크립트 하나로 처리해 동일 토큰의 동시 재발급을 방지
    @Override
    public boolean consume(String tokenId, String tokenHash) {
        Long deleted = redisTemplate.execute(
                CONSUME_SCRIPT,
                Collections.singletonList(key(tokenId)),
                tokenHash
        );
        return Long.valueOf(1L).equals(deleted);
    }

    private String key(String tokenId) {
        if (tokenId == null || tokenId.trim().isEmpty()) {
            throw new IllegalArgumentException("Refresh Token ID must not be blank");
        }
        return KEY_PREFIX + tokenId;
    }
}
