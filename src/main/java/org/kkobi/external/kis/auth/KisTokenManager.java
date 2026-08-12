package org.kkobi.external.kis.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.kkobi.exception.KisApiException;
import org.kkobi.external.kis.config.KisApiConfig.KisApiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Log4j2
public class KisTokenManager {

    private static final String TOKEN_KEY = "kis:access_token";
    private static final String LOCK_KEY = "kis:token_lock";
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final long LOCK_MAX_WAIT_MS = 10_000L;
    private static final long LOCK_POLL_INTERVAL_MS = 100L;
    private static final String TOKEN_ENDPOINT = "/oauth2/tokenP";

    // 본인 lockId인 경우에만 삭제하는 Lua 스크립트 (원자적 CAS 삭제)
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setScriptText(
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "  return redis.call('del', KEYS[1]) " +
                "else " +
                "  return 0 " +
                "end"
        );
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    private final RestTemplate restTemplate;
    private final KisApiProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KisTokenManager(
            @Qualifier("kisRestTemplate") RestTemplate restTemplate,
            KisApiProperties properties,
            StringRedisTemplate redisTemplate) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    public String getAccessToken() {
        String cached = redisTemplate.opsForValue().get(TOKEN_KEY);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }
        return issueWithLock();
    }

    private String issueWithLock() {
        String lockId = UUID.randomUUID().toString();
        long deadline = System.currentTimeMillis() + LOCK_MAX_WAIT_MS;

        while (System.currentTimeMillis() < deadline) {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, lockId, LOCK_TTL);

            if (Boolean.TRUE.equals(acquired)) {
                // 락 획득 성공 → double-check
                try {
                    String rechecked = redisTemplate.opsForValue().get(TOKEN_KEY);
                    if (rechecked != null && !rechecked.isBlank()) {
                        return rechecked;
                    }
                    return issueAndCache();
                } finally {
                    releaseLock(lockId);
                }
            }

            // 락 획득 실패 → 폴링 대기 후 토큰 재확인
            try {
                Thread.sleep(LOCK_POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new KisApiException("KIS 토큰 발급 락 대기 중 인터럽트 발생");
            }

            String polled = redisTemplate.opsForValue().get(TOKEN_KEY);
            if (polled != null && !polled.isBlank()) {
                return polled;
            }
        }

        log.error("KIS 토큰 발급 락 타임아웃");
        throw new KisApiException("KIS 토큰 발급 락 타임아웃");
    }

    private void releaseLock(String lockId) {
        redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(LOCK_KEY), lockId);
    }

    private String issueAndCache() {
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("appkey", properties.appKey());
        body.put("appsecret", properties.appSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = properties.baseUrl() + TOKEN_ENDPOINT;
        log.debug("KIS 토큰 발급 요청 url={}", url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.POST,
                    new HttpEntity<>(body, headers), String.class);

            JsonNode json = objectMapper.readTree(response.getBody());
            String accessToken = textOrNull(json, "access_token");
            long expiresIn = json.path("expires_in").asLong(0);

            if (accessToken == null || accessToken.isBlank()) {
                throw new KisApiException("KIS 토큰 응답에 access_token이 없습니다.");
            }

            long ttlSeconds = Math.max(60L, expiresIn - 600);
            redisTemplate.opsForValue().set(TOKEN_KEY, accessToken, Duration.ofSeconds(ttlSeconds));

            log.info("KIS 토큰 발급 성공 expires_in={}s redis_ttl={}s", expiresIn, ttlSeconds);
            return accessToken;
        } catch (RestClientException ex) {
            log.error("KIS 토큰 발급 실패", ex);
            throw new KisApiException("KIS 토큰 발급 요청에 실패했습니다.", ex);
        } catch (KisApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("KIS 토큰 발급 실패", ex);
            throw new KisApiException("KIS 토큰 응답 파싱에 실패했습니다.", ex);
        }
    }

    public void invalidate() {
        redisTemplate.delete(TOKEN_KEY);
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
