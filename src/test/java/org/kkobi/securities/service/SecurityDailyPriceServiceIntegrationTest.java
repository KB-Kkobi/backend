package org.kkobi.securities.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {SecurityDailyPriceTestConfig.class})
class SecurityDailyPriceServiceIntegrationTest {

    @Autowired
    private SecurityDailyPriceService dailyPriceService;

    @Test
    void fetchAndSaveToday() {
        dailyPriceService.fetchAndSave(LocalDate.of(2026, 8, 19));
    }
}
