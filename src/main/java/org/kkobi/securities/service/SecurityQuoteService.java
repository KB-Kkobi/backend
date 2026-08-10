package org.kkobi.securities.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.kkobi.exception.KisApiException;
import org.kkobi.external.kis.client.KisApiClient;
import org.kkobi.external.kis.dto.StockPriceResponse;
import org.kkobi.securities.dto.request.QuoteRequest;
import org.kkobi.securities.dto.response.QuoteItem;
import org.kkobi.securities.dto.response.QuoteResponse;
import org.kkobi.securities.dto.response.TickerKisCodeRow;
import org.kkobi.securities.mapper.SecurityMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@Log4j2
public class SecurityQuoteService {

    private static final int MAX_TICKERS = 100;
    private static final String CACHE_PREFIX = "securities:quote:";
    private static final Duration CACHE_TTL = Duration.ofSeconds(10);

    private final SecurityMapper securityMapper;
    private final KisApiClient kisApiClient;
    private final StringRedisTemplate redisTemplate;
    private final ExecutorService kisQuoteExecutor;
    private final ObjectMapper mapper;

    public SecurityQuoteService(
            SecurityMapper securityMapper,
            KisApiClient kisApiClient,
            StringRedisTemplate redisTemplate,
            @Qualifier("kisQuoteExecutor") ExecutorService kisQuoteExecutor,
            @Qualifier("quoteObjectMapper") ObjectMapper mapper) {
        this.securityMapper = securityMapper;
        this.kisApiClient = kisApiClient;
        this.redisTemplate = redisTemplate;
        this.kisQuoteExecutor = kisQuoteExecutor;
        this.mapper = mapper;
    }

    public QuoteResponse getQuotes(QuoteRequest request) {
        List<String> tickers = normalizeTickers(request);

        Map<String, String> tickerToKisCode = loadKisCodes(tickers);
        Map<String, CachedQuote> cacheHits = readCache(tickerToKisCode.values());
        Set<String> missingKisCodes = collectMissing(tickerToKisCode.values(), cacheHits);
        Map<String, FetchResult> freshByKisCode = fetchAndCache(missingKisCodes);

        List<QuoteItem> quotes = new ArrayList<>(tickers.size());
        for (String ticker : tickers) {
            String kisCode = tickerToKisCode.get(ticker);
            if (kisCode == null) {
                quotes.add(QuoteItem.unsupported(ticker));
                continue;
            }
            CachedQuote cached = cacheHits.get(kisCode);
            if (cached != null) {
                quotes.add(cached.toQuoteItem(ticker, kisCode));
                continue;
            }
            FetchResult fresh = freshByKisCode.get(kisCode);
            if (fresh == null) {
                quotes.add(QuoteItem.failed(ticker, kisCode, "시세 조회 실패"));
            } else if (fresh.error() != null) {
                quotes.add(QuoteItem.failed(ticker, kisCode, fresh.error()));
            } else {
                quotes.add(fresh.quote().toQuoteItem(ticker, kisCode));
            }
        }
        return new QuoteResponse(quotes);
    }

    private List<String> normalizeTickers(QuoteRequest request) {
        if (request == null || request.getTickers() == null || request.getTickers().isEmpty()) {
            throw new IllegalArgumentException("tickers는 1개 이상이어야 합니다.");
        }
        Set<String> distinct = new LinkedHashSet<>();
        for (String raw : request.getTickers()) {
            if (raw == null) continue;
            String trimmed = raw.trim();
            if (!trimmed.isEmpty()) {
                distinct.add(trimmed);
            }
        }
        if (distinct.isEmpty()) {
            throw new IllegalArgumentException("tickers는 1개 이상이어야 합니다.");
        }
        if (distinct.size() > MAX_TICKERS) {
            throw new IllegalArgumentException("tickers는 최대 " + MAX_TICKERS + "개까지 요청할 수 있습니다.");
        }
        return new ArrayList<>(distinct);
    }

    private Map<String, String> loadKisCodes(List<String> tickers) {
        List<TickerKisCodeRow> rows = securityMapper.findKisCodesByTickers(tickers);
        Map<String, String> result = new LinkedHashMap<>(tickers.size());
        for (String ticker : tickers) {
            result.put(ticker, null);
        }
        for (TickerKisCodeRow row : rows) {
            result.put(row.getTicker(), row.getKisCode());
        }
        return result;
    }

    private Map<String, CachedQuote> readCache(Iterable<String> kisCodes) {
        List<String> keys = new ArrayList<>();
        List<String> codes = new ArrayList<>();
        for (String code : kisCodes) {
            if (code == null) continue;
            keys.add(CACHE_PREFIX + code);
            codes.add(code);
        }
        if (keys.isEmpty()) return Collections.emptyMap();

        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        Map<String, CachedQuote> result = new HashMap<>();
        if (values == null) return result;
        for (int i = 0; i < codes.size(); i++) {
            String raw = values.get(i);
            if (raw == null) continue;
            try {
                result.put(codes.get(i), mapper.readValue(raw, CachedQuote.class));
            } catch (Exception ex) {
                log.warn("시세 캐시 역직렬화 실패 code={} err={}", codes.get(i), ex.getMessage());
            }
        }
        return result;
    }

    private Set<String> collectMissing(Iterable<String> kisCodes, Map<String, CachedQuote> hits) {
        Set<String> missing = new LinkedHashSet<>();
        for (String code : kisCodes) {
            if (code != null && !hits.containsKey(code)) {
                missing.add(code);
            }
        }
        return missing;
    }

    private Map<String, FetchResult> fetchAndCache(Set<String> kisCodes) {
        if (kisCodes.isEmpty()) return Collections.emptyMap();

        List<CompletableFuture<Map.Entry<String, FetchResult>>> futures = new ArrayList<>(kisCodes.size());
        for (String code : kisCodes) {
            futures.add(CompletableFuture.supplyAsync(() -> Map.entry(code, fetchOne(code)), kisQuoteExecutor));
        }
        Map<String, FetchResult> result = new HashMap<>();
        for (CompletableFuture<Map.Entry<String, FetchResult>> f : futures) {
            try {
                Map.Entry<String, FetchResult> entry = f.join();
                result.put(entry.getKey(), entry.getValue());
            } catch (Exception ex) {
                log.warn("시세 병렬 조회 예외: {}", ex.getMessage());
            }
        }
        return result;
    }

    private FetchResult fetchOne(String kisCode) {
        try {
            Map<String, String> output = kisApiClient.fetchCurrentPrice(kisCode);
            StockPriceResponse price = StockPriceResponse.fromKisOutput(kisCode, output);
            CachedQuote cached = new CachedQuote(
                    price.price(), price.change(), price.changeRate(), price.prevClose(), Instant.now());
            writeCache(kisCode, cached);
            return new FetchResult(cached, null);
        } catch (KisApiException ex) {
            log.warn("KIS 시세 조회 실패 code={} err={}", kisCode, ex.getMessage());
            return new FetchResult(null, ex.getMessage());
        } catch (Exception ex) {
            log.warn("시세 조회 처리 실패 code={} err={}", kisCode, ex.getMessage());
            return new FetchResult(null, "시세 조회 실패");
        }
    }

    private record FetchResult(CachedQuote quote, String error) {}

    private void writeCache(String kisCode, CachedQuote value) {
        try {
            String json = mapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(CACHE_PREFIX + kisCode, json, CACHE_TTL);
        } catch (Exception ex) {
            log.warn("시세 캐시 저장 실패 code={} err={}", kisCode, ex.getMessage());
        }
    }

    // Redis에 저장하는 시세 캐시 값 (record는 Jackson으로 (역)직렬화)
    public record CachedQuote(
            java.math.BigDecimal price,
            java.math.BigDecimal change,
            java.math.BigDecimal changeRate,
            java.math.BigDecimal prevClose,
            Instant asOf
    ) {
        QuoteItem toQuoteItem(String ticker, String kisCode) {
            return QuoteItem.success(ticker, kisCode, price, change, changeRate, prevClose, asOf);
        }
    }
}
