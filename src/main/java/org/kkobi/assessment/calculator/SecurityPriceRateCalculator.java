package org.kkobi.assessment.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class SecurityPriceRateCalculator {

    private static final BigDecimal PERCENTAGE = BigDecimal.valueOf(100);
    private static final int RATE_SCALE = 4;

    public BigDecimal calculatePriceChangeRate(
            BigDecimal previousClosePrice,
            BigDecimal currentClosePrice) {
        if (previousClosePrice == null
                || currentClosePrice == null
                || previousClosePrice.signum() == 0) {
            return null;
        }

        return currentClosePrice.subtract(previousClosePrice)
                .multiply(PERCENTAGE)
                .divide(previousClosePrice, RATE_SCALE, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateDailyPriceRangeRate(
            BigDecimal openPrice,
            BigDecimal highPrice,
            BigDecimal lowPrice) {
        if (openPrice == null
                || highPrice == null
                || lowPrice == null
                || openPrice.signum() == 0) {
            return null;
        }

        return highPrice.subtract(lowPrice)
                .multiply(PERCENTAGE)
                .divide(openPrice, RATE_SCALE, RoundingMode.HALF_UP);
    }
}
