package org.kkobi.external.kis.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kkobi.exception.KisApiException;
import org.kkobi.external.kis.auth.KisTokenManager;
import org.kkobi.external.kis.config.KisApiConfig.KisApiProperties;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KisApiClientRetryTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KisTokenManager tokenManager;

    private KisApiProperties properties;
    private KisApiClient kisApiClient;

    @BeforeEach
    void setUp() {
        when(tokenManager.getAccessToken()).thenReturn("tok");
        properties = new KisApiProperties(
                "https://openapi.koreainvestment.com:9443",
                "test-app-key",
                "test-app-secret",
                "ws://test-ws"
        );
        // KisApiProperties가 record이므로 직접 인스턴스 생성
        kisApiClient = new KisApiClient(restTemplate, tokenManager, properties);
    }

    @Test
    @DisplayName("EGW00123 응답 시 토큰 무효화 후 1회 재시도한다")
    void callInternal_invalidatesAndRetries_whenEgw00123InHttpErrorBody() {
        String egwBody = "{\"rt_cd\":\"1\",\"msg_cd\":\"EGW00123\",\"msg1\":\"기간이 만료된 token 입니다.\"}";
        HttpServerErrorException egwException = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "error",
                HttpHeaders.EMPTY, egwBody.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        String successBody = "{\"rt_cd\":\"0\",\"output\":{" +
                "\"stck_prpr\":\"70000\"," +
                "\"prdy_vrss\":\"100\"," +
                "\"prdy_ctrt\":\"0.14\"," +
                "\"acml_vol\":\"1000000\"," +
                "\"stck_oprc\":\"69000\"," +
                "\"stck_hgpr\":\"70500\"," +
                "\"stck_lwpr\":\"68500\"," +
                "\"stck_sdpr\":\"69900\"}}";

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(egwException)
                .thenReturn(new ResponseEntity<>(successBody, HttpStatus.OK));

        Map<String, String> result = kisApiClient.fetchCurrentPrice("005930");

        verify(tokenManager, times(1)).invalidate();
        verify(restTemplate, times(2)).exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class));
        assertThat(result.get("stck_prpr"), equalTo("70000"));
    }

    @Test
    @DisplayName("재시도 후에도 EGW00123이면 KisApiException을 던진다")
    void callInternal_throwsKisApiException_whenEgw00123PersistsAfterRetry() {
        String egwBody = "{\"rt_cd\":\"1\",\"msg_cd\":\"EGW00123\",\"msg1\":\"기간이 만료된 token 입니다.\"}";
        HttpServerErrorException egwException = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR, "error",
                HttpHeaders.EMPTY, egwBody.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenThrow(egwException)
                .thenThrow(egwException);

        assertThrows(KisApiException.class, () -> kisApiClient.fetchCurrentPrice("005930"));

        // 무한 루프 방지: invalidate는 최초 1회만
        verify(tokenManager, times(1)).invalidate();
        verify(restTemplate, times(2)).exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class));
    }

    @Test
    @DisplayName("EGW00123이 아닌 오류면 재시도하지 않는다")
    void callInternal_doesNotRetry_whenErrorCodeIsNotEgw00123() {
        String otherErrorBody = "{\"rt_cd\":\"1\",\"msg_cd\":\"OTHER\",\"msg1\":\"다른 오류\"}";

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class)))
                .thenReturn(new ResponseEntity<>(otherErrorBody, HttpStatus.OK));

        assertThrows(KisApiException.class, () -> kisApiClient.fetchCurrentPrice("005930"));

        verify(tokenManager, never()).invalidate();
        verify(restTemplate, times(1)).exchange(any(URI.class), eq(HttpMethod.GET), any(), eq(String.class));
    }
}
