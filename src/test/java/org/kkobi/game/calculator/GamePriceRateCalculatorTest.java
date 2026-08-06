package org.kkobi.game.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.calculator.SecurityPriceRateCalculator;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.dto.ScenarioTickDto;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GamePriceRateCalculatorTest {

    private final GamePriceRateCalculator gamePriceRateCalculator = new GamePriceRateCalculator(
            new SecurityPriceRateCalculator()
    );

    @Test
    @DisplayName("시작 대비 등락률을 이전 tick 대비 등락률로 환산한다.")
    void calculateTickPriceChangeRate() {
        ScenarioDto scenario = new ScenarioDto();
        scenario.setTicks(List.of(
                createScenarioTick(25, -10.1),
                createScenarioTick(26, -7.3)
        ));

        BigDecimal priceChangeRate = gamePriceRateCalculator.calculateTickPriceChangeRate(
                scenario,
                26
        );

        assertEquals(0, new BigDecimal("3.1146").compareTo(priceChangeRate));
    }

    @Test
    @DisplayName("첫 tick의 이전 tick 대비 등락률은 0으로 계산한다.")
    void calculateFirstTickPriceChangeRate() {
        ScenarioDto scenario = new ScenarioDto();
        scenario.setTicks(List.of(createScenarioTick(0, 0.0)));

        BigDecimal priceChangeRate = gamePriceRateCalculator.calculateTickPriceChangeRate(
                scenario,
                0
        );

        assertEquals(0, BigDecimal.ZERO.compareTo(priceChangeRate));
    }

    private ScenarioTickDto createScenarioTick(int tick, double changeRate) {
        ScenarioTickDto scenarioTick = new ScenarioTickDto();
        scenarioTick.setTick(tick);
        scenarioTick.setChangeRate(changeRate);
        return scenarioTick;
    }
}
