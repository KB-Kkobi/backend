package org.kkobi.assessment.calculator;

import org.kkobi.assessment.enums.MarketState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MarketStateCalculator {

    private static final BigDecimal CRASH_RATE = BigDecimal.valueOf(-5);
    private static final BigDecimal BULL_RATE = BigDecimal.valueOf(3);
    private static final BigDecimal VOLATILE_RATE = BigDecimal.valueOf(5);

    public MarketState calculateMarketState(
            BigDecimal currentPriceChangeRate,
            BigDecimal dailyPriceRangeRate) {
        if (dailyPriceRangeRate != null
                && dailyPriceRangeRate.compareTo(VOLATILE_RATE) >= 0) {
            return MarketState.VOLATILE;
        }

        if (currentPriceChangeRate != null
                && currentPriceChangeRate.compareTo(CRASH_RATE) <= 0) {
            return MarketState.CRASH;
        }

        if (currentPriceChangeRate != null
                && currentPriceChangeRate.compareTo(BULL_RATE) >= 0) {
            return MarketState.BULL;
        }

        return MarketState.NORMAL;
    }
}
