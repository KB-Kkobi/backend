package org.kkobi.game.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.dto.ScenarioTickDto;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GameSecurityReturnCalculatorTest {

    private final GameSecurityReturnCalculator gameSecurityReturnCalculator =
            new GameSecurityReturnCalculator();

    @Test
    @DisplayName("초기 배분과 추가 매수 원금으로 현재 수익률을 계산한다.")
    void calculateCurrentReturnRate() {
        ScenarioDto scenario = createScenario();
        ActionLogDto initialAllocation = createActionLog(1L, 0, 1_000L);
        ActionLogDto additionalBuy = createActionLog(2L, 1, 1_500L);

        BigDecimal currentReturnRate = gameSecurityReturnCalculator.calculateCurrentReturnRate(
                scenario,
                2,
                List.of(initialAllocation, additionalBuy)
        );

        assertEquals(0, new BigDecimal("-20.00").compareTo(currentReturnRate));
    }

    @Test
    @DisplayName("전량 매도 후에는 이전 보유 구간의 수익률을 사용하지 않는다.")
    void calculateCurrentReturnRateReturnsNullAfterFullSell() {
        ScenarioDto scenario = createScenario();
        ActionLogDto initialAllocation = createActionLog(1L, 0, 1_000L);
        ActionLogDto fullSell = createActionLog(2L, 1, 0L);

        BigDecimal currentReturnRate = gameSecurityReturnCalculator.calculateCurrentReturnRate(
                scenario,
                2,
                List.of(initialAllocation, fullSell)
        );

        assertNull(currentReturnRate);
    }

    private ScenarioDto createScenario() {
        ScenarioDto scenario = new ScenarioDto();
        scenario.setTicks(List.of(
                createScenarioTick(0, 100L),
                createScenarioTick(1, 50L),
                createScenarioTick(2, 60L)
        ));
        return scenario;
    }

    private ScenarioTickDto createScenarioTick(int tick, long price) {
        ScenarioTickDto scenarioTick = new ScenarioTickDto();
        scenarioTick.setTick(tick);
        scenarioTick.setPrice(price);
        return scenarioTick;
    }

    private ActionLogDto createActionLog(
            Long actionLogId,
            int gameTick,
            long currentStock) {
        ActionLogDto actionLog = new ActionLogDto();
        actionLog.setActionLogId(actionLogId);
        actionLog.setGameTick(gameTick);
        actionLog.setCurrentStock(currentStock);
        return actionLog;
    }
}
