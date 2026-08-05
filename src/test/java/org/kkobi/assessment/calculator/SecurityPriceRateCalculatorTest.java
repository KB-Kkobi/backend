package org.kkobi.assessment.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityPriceRateCalculatorTest {

    private final SecurityPriceRateCalculator securityPriceRateCalculator =
            new SecurityPriceRateCalculator();

    @Test
    @DisplayName("종가 등락률과 당일 고가·저가 변동폭을 계산한다.")
    void calculateSecurityPriceRates() {
        BigDecimal priceChangeRate = securityPriceRateCalculator.calculatePriceChangeRate(
                new BigDecimal("10000"),
                new BigDecimal("9500")
        );
        BigDecimal dailyPriceRangeRate = securityPriceRateCalculator.calculateDailyPriceRangeRate(
                new BigDecimal("10000"),
                new BigDecimal("10500"),
                new BigDecimal("9500")
        );

        assertEquals(0, new BigDecimal("-5.0000").compareTo(priceChangeRate));
        assertEquals(0, new BigDecimal("10.0000").compareTo(dailyPriceRangeRate));
    }
}
