package org.kkobi.external.kis.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.kkobi.exception.KisApiException;
import org.kkobi.external.kis.config.KisApiConfig.KisApiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@Log4j2
public class KisTokenManager {

    private static final String REDIS_KEY = "kis:access_token";
    private static final Duration TTL_SAFETY_MARGIN = Duration.ofMinutes(10);
    private static final String TOKEN_ENDPOINT = "/oauth2/tokenP";

    private final RestTemplate restTemplate;
    private final KisApiProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Object issueLock = new Object();

    public KisTokenManager(
            @Qualifier("kisRestTemplate") RestTemplate restTemplate,
            KisApiProperties properties,
            StringRedisTemplate redisTemplate) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    public String getAccessToken() {
        String cached = redisTemplate.opsForValue().get(REDIS_KEY);
        if (cached != null && !cached.isBlank()) {
            return cached;
        }

        synchronized (issueLock) {
            String rechecked = redisTemplate.opsForValue().get(REDIS_KEY);
            if (rechecked != null && !rechecked.isBlank()) {
                return rechecked;
            }
            return issueAndCache();
        }
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

            long ttlSeconds = Math.max(60L, expiresIn - TTL_SAFETY_MARGIN.getSeconds());
            redisTemplate.opsForValue().set(REDIS_KEY, accessToken, Duration.ofSeconds(ttlSeconds));

            log.debug("KIS 토큰 발급 성공 token={} ttlSec={}", mask(accessToken), ttlSeconds);
            return accessToken;
        } catch (RestClientException ex) {
            throw new KisApiException("KIS 토큰 발급 요청에 실패했습니다.", ex);
        } catch (KisApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new KisApiException("KIS 토큰 응답 파싱에 실패했습니다.", ex);
        }
    }

    // 강제 재발급 (401 등 만료 상황에서 호출 예정, 이번 범위에서는 사용하지 않음)
    public void invalidate() {
        redisTemplate.delete(REDIS_KEY);
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    private static String mask(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
}
