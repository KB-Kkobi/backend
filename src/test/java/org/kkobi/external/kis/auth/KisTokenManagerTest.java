package org.kkobi.external.kis.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.external.kis.config.KisApiConfig.KisApiProperties;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KisTokenManagerTest {

    private static final String TOKEN_KEY = "kis:access_token";
    private static final String LOCK_KEY = "kis:token_lock";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private KisApiProperties properties;
    private KisTokenManager kisTokenManager;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        properties = new KisApiProperties(
                "https://openapi.koreainvestment.com:9443",
                "test-app-key",
                "test-app-secret",
                "ws://test-ws"
        );
        kisTokenManager = new KisTokenManager(restTemplate, properties, redisTemplate);
    }

    @Test
    @DisplayName("Redis에 유효한 토큰이 있으면 KIS 호출 없이 반환한다")
    void getAccessToken_returnsCachedToken_whenRedisHasValidToken() {
        when(valueOps.get(TOKEN_KEY)).thenReturn("valid_token");

        String result = kisTokenManager.getAccessToken();

        assertEquals("valid_token", result);
        verify(restTemplate, never()).exchange(
                anyString(), any(HttpMethod.class), any(), eq(String.class));
    }

    @Test
    @DisplayName("Redis가 비어 있으면 KIS에서 토큰을 발급하고 저장한다")
    void getAccessToken_issuesNewToken_whenRedisIsEmpty() {
        // 최초 캐시 조회 및 double-check 모두 null 반환
        when(valueOps.get(TOKEN_KEY)).thenReturn(null);
        // 락 획득 성공
        when(valueOps.setIfAbsent(eq(LOCK_KEY), anyString(), eq(Duration.ofSeconds(10)))).thenReturn(true);
        // 락 해제용 Lua 스크립트 실행 (raw type은 Mockito API 한계로 unchecked 경고 발생)
        doReturn(1L).when(redisTemplate).execute(any(DefaultRedisScript.class), anyList(), any());

        String tokenJson = "{\"access_token\":\"new_token\",\"expires_in\":86400}";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(tokenJson, HttpStatus.OK));

        String result = kisTokenManager.getAccessToken();

        assertEquals("new_token", result);
        verify(valueOps).set(eq(TOKEN_KEY), eq("new_token"), any(Duration.class));
    }

    @Test
    @DisplayName("락 대기 중 다른 인스턴스가 토큰을 발급하면 그것을 반환한다")
    void getAccessToken_returnsTokenIssuedByOther_whenLockIsHeldByOther() {
        // 최초 캐시 조회: null, 폴링 후 재확인: "issued_by_other"
        when(valueOps.get(TOKEN_KEY))
                .thenReturn(null)
                .thenReturn("issued_by_other");
        // 락 획득 실패
        when(valueOps.setIfAbsent(eq(LOCK_KEY), anyString(), eq(Duration.ofSeconds(10)))).thenReturn(false);

        String result = kisTokenManager.getAccessToken();

        assertEquals("issued_by_other", result);
        verify(restTemplate, never()).exchange(
                anyString(), any(HttpMethod.class), any(), eq(String.class));
    }

    @Test
    @DisplayName("EGW00123 응답 시 invalidate 후 재발급하면 토큰이 갱신된다")
    void invalidate_deletesTokenKey_fromRedis() {
        kisTokenManager.invalidate();

        verify(redisTemplate).delete(TOKEN_KEY);
    }
}
