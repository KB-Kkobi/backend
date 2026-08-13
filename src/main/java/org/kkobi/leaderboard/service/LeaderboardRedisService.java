package org.kkobi.leaderboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.kkobi.leaderboard.dto.LeaderboardCacheDto;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class LeaderboardRedisService {

    private static final String USER_CACHE_PREFIX = "leaderboard:user:";
    private static final String PERSONA_RANKING_PREFIX = "leaderboard:persona:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(3);

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 사용자 리더보드 정보를 Redis에 저장
    public void saveUserCache(LeaderboardCacheDto cacheDto){
        try{
            String key = USER_CACHE_PREFIX + cacheDto.getUserId();
            String value = objectMapper.writeValueAsString(cacheDto);

            redisTemplate.opsForValue()
                    .set(key, value, CACHE_TTL);
        } catch (Exception e){
            log.warn(
                    "리더보드 사용자 캐시 저장 실패 userId={} error={}",
                    cacheDto.getUserId(),
                    e.getMessage()
            );
        }
    }

    // 여러 사용자의 리더보드 정보를 Redis에서 한 번에 조회
    public List<LeaderboardCacheDto> getUserCaches(List<Long> userIds){
        if(userIds == null || userIds.isEmpty()){
            return Collections.emptyList();
        }

        List<String> keys = new ArrayList<>();

        for(Long userId : userIds) {
            keys.add(USER_CACHE_PREFIX + userId);
        }

        List<String> values = redisTemplate.opsForValue().multiGet(keys);

        if(values == null){
            return Collections.emptyList();
        }

        List<LeaderboardCacheDto> result = new ArrayList<>();

        for(String value : values){
            if(value == null){
                continue;
            }

            try {
                LeaderboardCacheDto cacheDto =
                        objectMapper.readValue(value, LeaderboardCacheDto.class);

                result.add(cacheDto);
            } catch (Exception e){
                log.warn(
                        "리더보드 사용자 캐시 조회 실패 error={}",
                        e.getMessage()
                );
            }
        }

        return result;
    }

    // 성향별 리더보드에 사용자 수익률을 저장
    public void savePersonaRanking(
            Long personaId,
            Long userId,
            BigDecimal returnRate
    ) {
        if (personaId == null || userId == null || returnRate == null){
            return;
        }

        String key = PERSONA_RANKING_PREFIX + personaId;

        redisTemplate.opsForZSet()
                .add(
                        key,
                        userId.toString(),
                        returnRate.doubleValue()
                );

        redisTemplate.expire(key, CACHE_TTL);
    }
}
