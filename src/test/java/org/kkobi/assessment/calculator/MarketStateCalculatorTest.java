package org.kkobi.assessment.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.enums.MarketState;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketStateCalculatorTest {

    private final MarketStateCalculator marketStateCalculator = new MarketStateCalculator();

    @Test
    @DisplayName("급락, 급등, 변동성, 평범장 순서로 시장 상태를 판정한다.")
    void calculateMarketState() {
        assertEquals(
                MarketState.CRASH,
                marketStateCalculator.calculateMarketState(
                        new BigDecimal("-5.00"),
                        new BigDecimal("8.00")
                )
        );
        assertEquals(
                MarketState.BULL,
                marketStateCalculator.calculateMarketState(
                        new BigDecimal("3.00"),
                        new BigDecimal("8.00")
                )
        );
        assertEquals(
                MarketState.VOLATILE,
                marketStateCalculator.calculateMarketState(
                        new BigDecimal("1.00"),
                        new BigDecimal("5.00")
                )
        );
        assertEquals(
                MarketState.NORMAL,
                marketStateCalculator.calculateMarketState(
                        new BigDecimal("1.00"),
                        new BigDecimal("2.00")
                )
        );
        assertEquals(
                MarketState.CRASH,
                marketStateCalculator.calculateMarketState(
                        new BigDecimal("-6.00"),
                        null
                )
        );
    }

    @Test
    @DisplayName("시장 상태 임계값의 직전과 경계를 구분한다.")
    void calculateMarketStateThresholdBoundaries() {
        assertEquals(
                MarketState.NORMAL,
                marketStateCalculator.calculateMarketState(
                        new BigDecimal("-4.99"),
                        new BigDecimal("4.99")
                )
        );
        assertEquals(
                MarketState.CRASH,
                marketStateCalculator.calculateMarketState(
                        new BigDecimal("-5.00"),
                        new BigDecimal("4.99")
                )
        );
        assertEquals(
                MarketState.NORMAL,
                marketStateCalculator.calculateMarketState(
                        new BigDecimal("2.99"),
                        new BigDecimal("4.99")
                )
        );
        assertEquals(
                MarketState.BULL,
                marketStateCalculator.calculateMarketState(
                        new BigDecimal("3.00"),
                        new BigDecimal("4.99")
                )
        );
        assertEquals(
                MarketState.VOLATILE,
                marketStateCalculator.calculateMarketState(
                        BigDecimal.ZERO,
                        new BigDecimal("5.00")
                )
        );
    }
}
