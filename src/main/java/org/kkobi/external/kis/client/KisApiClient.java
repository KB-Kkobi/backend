package org.kkobi.external.kis.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.kkobi.exception.KisApiException;
import org.kkobi.external.kis.auth.KisTokenManager;
import org.kkobi.external.kis.config.KisApiConfig.KisApiProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Log4j2
public class KisApiClient {

    private static final String MARKET_DIV_STOCK = "J";
    private static final String TR_ID_PRICE = "FHKST01010100";
    private static final String TR_ID_DAILY_CHART = "FHKST03010100";
    private static final String PATH_PRICE = "/uapi/domestic-stock/v1/quotations/inquire-price";
    private static final String PATH_DAILY_CHART = "/uapi/domestic-stock/v1/quotations/inquire-daily-itemchartprice";
    private static final String ERROR_CODE_TOKEN_EXPIRED = "EGW00123";

    private final RestTemplate restTemplate;
    private final KisTokenManager tokenManager;
    private final KisApiProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public KisApiClient(
            @Qualifier("kisRestTemplate") RestTemplate restTemplate,
            KisTokenManager tokenManager,
            KisApiProperties properties) {
        this.restTemplate = restTemplate;
        this.tokenManager = tokenManager;
        this.properties = properties;
    }

    public Map<String, String> fetchCurrentPrice(String stockCode) {
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.baseUrl())
                .path(PATH_PRICE)
                .queryParam("FID_COND_MRKT_DIV_CODE", MARKET_DIV_STOCK)
                .queryParam("FID_INPUT_ISCD", stockCode)
                .build()
                .encode()
                .toUri();

        JsonNode root = call(uri, TR_ID_PRICE);
        JsonNode output = root.get("output");
        if (output == null || output.isNull()) {
            throw new KisApiException("KIS 현재가 응답에 output이 없습니다.");
        }
        return toStringMap(output);
    }

    public List<Map<String, String>> fetchDailyChart(
            String stockCode, String fromYyyyMmDd, String toYyyyMmDd, String periodCode) {

        URI uri = UriComponentsBuilder.fromHttpUrl(properties.baseUrl())
                .path(PATH_DAILY_CHART)
                .queryParam("FID_COND_MRKT_DIV_CODE", MARKET_DIV_STOCK)
                .queryParam("FID_INPUT_ISCD", stockCode)
                .queryParam("FID_INPUT_DATE_1", fromYyyyMmDd)
                .queryParam("FID_INPUT_DATE_2", toYyyyMmDd)
                .queryParam("FID_PERIOD_DIV_CODE", periodCode)
                .queryParam("FID_ORG_ADJ_PRC", "0")
                .build()
                .encode()
                .toUri();

        JsonNode root = call(uri, TR_ID_DAILY_CHART);
        JsonNode output2 = root.get("output2");
        List<Map<String, String>> result = new ArrayList<>();
        if (output2 == null || !output2.isArray()) {
            return result;
        }
        for (JsonNode node : output2) {
            result.add(toStringMap(node));
        }
        return result;
    }

    private JsonNode call(URI uri, String trId) {
        return callInternal(uri, trId, false);
    }

    private JsonNode callInternal(URI uri, String trId, boolean isRetry) {
        HttpHeaders headers = buildHeaders(trId);
        log.debug("KIS 요청 uri={} tr_id={} isRetry={}", uri, trId, isRetry);

        ResponseEntity<String> response;
        try {
            response = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        } catch (HttpStatusCodeException ex) {
            String status = ex.getStatusCode().toString();
            String body = ex.getResponseBodyAsString();
            if (body.contains(ERROR_CODE_TOKEN_EXPIRED) && !isRetry) {
                log.warn("KIS 토큰 만료 감지(HTTP 오류) status={} body={} 토큰 무효화 후 재시도", status, body);
                tokenManager.invalidate();
                return callInternal(uri, trId, true);
            }
            log.error("KIS HTTP 오류 status={} body={}", status, body, ex);
            throw new KisApiException("KIS HTTP 오류 status=" + status + " body=" + body, ex);
        } catch (RestClientException ex) {
            log.error("KIS 요청 실패", ex);
            throw new KisApiException("KIS 요청에 실패했습니다.", ex);
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(response.getBody());
        } catch (Exception ex) {
            throw new KisApiException("KIS 응답 파싱에 실패했습니다.", ex);
        }

        String rtCd = textOrEmpty(root, "rt_cd");
        if (!"0".equals(rtCd)) {
            String msgCd = textOrEmpty(root, "msg_cd");
            String msg = textOrEmpty(root, "msg1");
            log.error("KIS 오류 tr_id={} rt_cd={} msg_cd={} msg1={}", trId, rtCd, msgCd, msg);
            if (ERROR_CODE_TOKEN_EXPIRED.equals(msgCd) && !isRetry) {
                log.warn("KIS 토큰 만료 감지(rt_cd 오류) 토큰 무효화 후 재시도");
                tokenManager.invalidate();
                return callInternal(uri, trId, true);
            }
            throw new KisApiException("KIS 오류 rt_cd=" + rtCd + " msg_cd=" + msgCd + " msg1=" + msg);
        }

        log.debug("KIS 응답 성공 tr_id={} rt_cd={}", trId, rtCd);
        return root;
    }

    private HttpHeaders buildHeaders(String trId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("authorization", "Bearer " + tokenManager.getAccessToken());
        headers.set("appkey", properties.appKey());
        headers.set("appsecret", properties.appSecret());
        headers.set("custtype", "P");
        headers.set("tr_id", trId);
        return headers;
    }

    private static Map<String, String> toStringMap(JsonNode node) {
        Map<String, String> map = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            JsonNode value = entry.getValue();
            map.put(entry.getKey(), value == null || value.isNull() ? "" : value.asText());
        }
        return map;
    }

    private static String textOrEmpty(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText();
    }
}
