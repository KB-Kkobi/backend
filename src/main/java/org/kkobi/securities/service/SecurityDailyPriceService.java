package org.kkobi.securities.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kkobi.external.kis.dto.CandleResponse;
import org.kkobi.external.kis.service.StockQuoteService;
import org.kkobi.securities.dto.SecurityDailyPriceRow;
import org.kkobi.securities.dto.SecurityKisRow;
import org.kkobi.securities.mapper.SecurityDailyPriceMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityDailyPriceService {

    private final SecurityDailyPriceMapper dailyPriceMapper;
    private final StockQuoteService stockQuoteService;

    public void fetchAndSave(LocalDate date) {
        List<SecurityKisRow> targets = dailyPriceMapper.findAllWithKisCode();
        if (targets.isEmpty()) {
            log.info("일일 주가 저장: kis_code 있는 종목 없음");
            return;
        }

        log.info("일일 주가 저장 시작: {}개 종목, 기준일={}", targets.size(), date);

        List<SecurityDailyPriceRow> rows = new ArrayList<>();
        for (SecurityKisRow security : targets) {
            try {
                List<CandleResponse> candles = stockQuoteService.getDailyChart(
                        security.getKisCode(), "D", date, date);

                if (candles.isEmpty()) {
                    log.debug("일봉 없음: securityId={}, kisCode={}, date={}",
                            security.getSecurityId(), security.getKisCode(), date);
                    continue;
                }

                CandleResponse candle = candles.get(0);
                rows.add(SecurityDailyPriceRow.builder()
                        .securityId(security.getSecurityId())
                        .tradeDate(candle.date())
                        .openPrice(candle.open())
                        .closePrice(candle.close())
                        .highPrice(candle.high())
                        .lowPrice(candle.low())
                        .volume(candle.volume())
                        .build());

                Thread.sleep(200); // KIS API 호출 제한 준수
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("일일 주가 저장 인터럽트: securityId={}", security.getSecurityId());
                break;
            } catch (Exception e) {
                log.warn("일봉 조회 실패: securityId={}, kisCode={}, error={}",
                        security.getSecurityId(), security.getKisCode(), e.getMessage());
            }
        }

        if (!rows.isEmpty()) {
            dailyPriceMapper.upsertBatch(rows);
            log.info("일일 주가 저장 완료: {}건 upsert", rows.size());
        }
    }
}
