package org.kkobi.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = RedisConfig.class)
@TestPropertySource(properties = {
        "redis.host=localhost",
        "redis.port=6379"
})
class RedisConnectionIntegrationTest {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Redis에 테스트 값을 저장하고 정상적으로 조회되는지 확인
    @Test
    void redisValueSaveAndFind(){
        String key = "test:redis:connection";
        String value = "connected";

        try {
            // Redis에 테스트 값을 저장
            stringRedisTemplate.opsForValue().set(key,value);

            // Redis에 저장된 값을 조회
            String savedValue =
                    stringRedisTemplate.opsForValue().get(key);

            assertEquals(value, savedValue);
        } finally {
            // 테스트가 끝난 뒤 테스트 데이터를 삭제
            stringRedisTemplate.delete(key);
        }
    }
}