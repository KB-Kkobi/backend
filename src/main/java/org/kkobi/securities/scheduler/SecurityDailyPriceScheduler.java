package org.kkobi.securities.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kkobi.securities.service.SecurityDailyPriceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityDailyPriceScheduler {

    private static final String AFTER_MARKET_CLOSE_CRON = "0 0 16 * * MON-FRI";
    private static final String KST_ZONE = "Asia/Seoul";

    private final SecurityDailyPriceService dailyPriceService;

    @Scheduled(cron = AFTER_MARKET_CLOSE_CRON, zone = KST_ZONE)
    public void saveDailyPrices() {
        log.info("일일 주가 저장 스케줄러 시작");
        dailyPriceService.fetchAndSave(LocalDate.now());
    }
}
