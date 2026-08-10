package org.kkobi.security.token;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.config.RedisConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RedisConfig.class)
@TestPropertySource(properties = {
        "redis.host=localhost",
        "redis.port=6379"
})
class RedisRefreshTokenStoreIntegrationTest {
    private static final String TOKEN_ID = "integration-test-token";
    private static final String TOKEN_HASH = "integration-test-hash";
    private static final String REDIS_KEY = "auth:refresh:" + TOKEN_ID;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private RedisRefreshTokenStore refreshTokenStore;

    @BeforeEach
    void setUp() {
        refreshTokenStore = new RedisRefreshTokenStore(redisTemplate);
    }

    @AfterEach
    void cleanUp() {
        redisTemplate.delete(REDIS_KEY);
    }

    @Test
    void savesWithTtlAndConsumesOnlyOnce() {
        refreshTokenStore.save(TOKEN_ID, TOKEN_HASH, Duration.ofMinutes(1));

        Long timeToLive = redisTemplate.getExpire(REDIS_KEY);
        assertNotNull(timeToLive);
        assertTrue(timeToLive > 0);
        assertTrue(refreshTokenStore.consume(TOKEN_ID, TOKEN_HASH));
        assertFalse(refreshTokenStore.consume(TOKEN_ID, TOKEN_HASH));
    }

    @Test
    void doesNotConsumeWhenHashDoesNotMatch() {
        refreshTokenStore.save(TOKEN_ID, TOKEN_HASH, Duration.ofMinutes(1));

        assertFalse(refreshTokenStore.consume(TOKEN_ID, "different-hash"));
        assertTrue(refreshTokenStore.consume(TOKEN_ID, TOKEN_HASH));
    }
}
