package org.kkobi.game.calculator;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.calculator.SecurityPriceRateCalculator;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.dto.ScenarioTickDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class GamePriceRateCalculator {

    private static final BigDecimal BASE_RATE = BigDecimal.valueOf(100);

    private final SecurityPriceRateCalculator securityPriceRateCalculator;

    public BigDecimal calculateTickPriceChangeRate(
            ScenarioDto scenario,
            int gameTick) {
        ScenarioTickDto currentTick = getScenarioTick(scenario, gameTick);
        ScenarioTickDto previousTick = scenario.getTicks()
                .stream()
                .filter(scenarioTick -> scenarioTick.getTick() < gameTick)
                .max(Comparator.comparingInt(ScenarioTickDto::getTick))
                .orElse(null);
        if (previousTick == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal previousStartRelativeRate = BASE_RATE.add(
                BigDecimal.valueOf(previousTick.getChangeRate())
        );
        BigDecimal currentStartRelativeRate = BASE_RATE.add(
                BigDecimal.valueOf(currentTick.getChangeRate())
        );
        return securityPriceRateCalculator.calculatePriceChangeRate(
                previousStartRelativeRate,
                currentStartRelativeRate
        );
    }

    private ScenarioTickDto getScenarioTick(ScenarioDto scenario, int gameTick) {
        if (scenario == null || scenario.getTicks() == null) {
            throw new IllegalArgumentException("게임 시나리오 tick 정보는 필수입니다.");
        }

        return scenario.getTicks()
                .stream()
                .filter(scenarioTick -> scenarioTick.getTick() == gameTick)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "게임 시나리오 tick을 찾을 수 없습니다: " + gameTick
                ));
    }
}
