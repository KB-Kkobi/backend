package org.kkobi.game.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.calculator.AssetRatioCalculator;
import org.kkobi.assessment.calculator.BehaviorContextFactory;
import org.kkobi.assessment.calculator.BehaviorRuleEngine;
import org.kkobi.assessment.calculator.MarketStateCalculator;
import org.kkobi.assessment.calculator.SecurityPriceRateCalculator;
import org.kkobi.game.calculator.GamePriceRateCalculator;
import org.kkobi.game.calculator.GameSecurityReturnCalculator;
import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.dto.GameBehaviorRequest;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.dto.ScenarioTickDto;
import org.kkobi.game.mapper.ActionLogMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameActionServiceScenarioDateTest {

    @Test
    @DisplayName("게임 보유 기간을 시나리오 tick 날짜의 일수 차이로 계산한다.")
    void saveGameActionLogCalculatesHoldingDaysFromScenarioDate() {
        InMemoryActionLogMapper actionLogMapper = new InMemoryActionLogMapper();
        GameActionService gameActionService = createGameActionService(actionLogMapper);

        gameActionService.saveGameActionLog(createGameBehaviorRequest(
                0,
                "INITIAL_ALLOCATION",
                "ALL",
                0L,
                1_000L
        ));
        gameActionService.saveGameActionLog(createGameBehaviorRequest(
                1,
                "SELL",
                "STOCK",
                1_000L,
                0L
        ));

        ActionLogDto sellActionLog = actionLogMapper.getActionLogsByUserId(1L).get(1);

        assertScoreEquals("0.00", sellActionLog.getRtScoreDelta());
        assertScoreEquals("-5.00", sellActionLog.getLhScoreDelta());
        assertScoreEquals("-5.00", sellActionLog.getRpScoreDelta());
    }

    private GameActionService createGameActionService(ActionLogMapper actionLogMapper) {
        MarketStateCalculator marketStateCalculator = new MarketStateCalculator();
        return new GameActionService(
                new ActionLogService(actionLogMapper),
                createScenarioService(),
                new GamePriceRateCalculator(new SecurityPriceRateCalculator()),
                new GameSecurityReturnCalculator(),
                new BehaviorContextFactory(
                        new AssetRatioCalculator(),
                        marketStateCalculator
                ),
                new BehaviorRuleEngine()
        );
    }

    private ScenarioService createScenarioService() {
        return new ScenarioService() {
            @Override
            public ScenarioDto getScenario(String scenarioId) {
                ScenarioDto scenario = new ScenarioDto();
                scenario.setScenarioId(scenarioId);
                scenario.setTotalTicks(1);
                scenario.setTicks(List.of(
                        createScenarioTick(0, "2026-01-01"),
                        createScenarioTick(1, "2026-02-10")
                ));
                return scenario;
            }
        };
    }

    private ScenarioTickDto createScenarioTick(int tick, String date) {
        ScenarioTickDto scenarioTick = new ScenarioTickDto();
        scenarioTick.setTick(tick);
        scenarioTick.setDate(date);
        scenarioTick.setPrice(100L);
        scenarioTick.setChangeRate(0.0);
        return scenarioTick;
    }

    private GameBehaviorRequest createGameBehaviorRequest(
            int tick,
            String actionType,
            String assetType,
            long currentCash,
            long currentStock) {
        GameBehaviorRequest request = new GameBehaviorRequest();
        request.setUserId(1L);
        request.setScenarioId("TEST");
        request.setTick(tick);
        request.setActionType(actionType);
        request.setAssetType(assetType);
        request.setActionAmount(1_000L);
        request.setCurrentCash(currentCash);
        request.setCurrentStock(currentStock);
        request.setCurrentDeposit(0L);
        return request;
    }

    private void assertScoreEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private static class InMemoryActionLogMapper implements ActionLogMapper {

        private final List<ActionLogDto> actionLogs = new ArrayList<>();

        @Override
        public int saveActionLog(ActionLogDto actionLog) {
            actionLog.setActionLogId((long) actionLogs.size() + 1);
            actionLogs.add(actionLog);
            return 1;
        }

        @Override
        public List<ActionLogDto> getActionLogsByUserId(Long userId) {
            return actionLogs.stream()
                    .filter(actionLog -> userId.equals(actionLog.getUserId()))
                    .toList();
        }
    }
}
