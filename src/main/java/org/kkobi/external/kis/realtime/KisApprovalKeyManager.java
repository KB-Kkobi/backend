package org.kkobi.external.kis.realtime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.kkobi.exception.KisApiException;
import org.kkobi.external.kis.config.KisApiConfig.KisApiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

// KIS 실시간 웹소켓 접근 승인키(approval_key)를 발급/캐싱한다.
// tokenP와 별개의 자격이며, 유효기간이 약 24시간이라 하루 1회 발급을 원칙으로 한다.
@Component
@Log4j2
public class KisApprovalKeyManager {

    private static final String REDIS_KEY = "kis:approval_key";
    private static final Duration TTL = Duration.ofHours(23);
    private static final String APPROVAL_ENDPOINT = "/oauth2/Approval";

    private final RestTemplate restTemplate;
    private final KisApiProperties properties;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Object issueLock = new Object();

    public KisApprovalKeyManager(
            @Qualifier("kisRestTemplate") RestTemplate restTemplate,
            KisApiProperties properties,
            StringRedisTemplate redisTemplate) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.redisTemplate = redisTemplate;
    }

    public String getApprovalKey() {
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

    public void invalidate() {
        redisTemplate.delete(REDIS_KEY);
    }

    private String issueAndCache() {
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("appkey", properties.appKey());
        body.put("secretkey", properties.appSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String url = properties.baseUrl() + APPROVAL_ENDPOINT;
        log.debug("KIS 승인키 발급 요청 url={}", url);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            JsonNode json = objectMapper.readTree(response.getBody());
            String approvalKey = json.path("approval_key").asText(null);

            if (approvalKey == null || approvalKey.isBlank()) {
                throw new KisApiException("KIS 승인키 응답에 approval_key가 없습니다.");
            }

            redisTemplate.opsForValue().set(REDIS_KEY, approvalKey, TTL);
            log.debug("KIS 승인키 발급 성공 approval_key={}", mask(approvalKey));
            return approvalKey;
        } catch (RestClientException ex) {
            throw new KisApiException("KIS 승인키 발급 요청에 실패했습니다.", ex);
        } catch (KisApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new KisApiException("KIS 승인키 응답 파싱에 실패했습니다.", ex);
        }
    }

    private static String mask(String value) {
        if (value == null || value.length() < 8) {
            return "***";
        }
        return value.substring(0, 4) + "***" + value.substring(value.length() - 4);
    }
}
