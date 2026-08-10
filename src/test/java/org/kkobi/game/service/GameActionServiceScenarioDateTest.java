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
import org.kkobi.game.dto.GameActionRequest;
import org.kkobi.game.dto.GameActionResponse;
import org.kkobi.game.dto.GameBehaviorRequest;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.dto.ScenarioTickDto;
import org.kkobi.game.mapper.ActionLogMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameActionServiceScenarioDateTest {

    @Test
    @DisplayName("게임 행동 요청을 기존 분석 흐름에 연결하고 저장 결과를 응답한다.")
    void saveGameActionReturnsSavedActionLog() {
        InMemoryActionLogMapper actionLogMapper = new InMemoryActionLogMapper();
        GameActionService gameActionService = createGameActionService(actionLogMapper);
        gameActionService.saveGameActionLog(createGameBehaviorRequest(
                0,
                "INITIAL_ALLOCATION",
                "ALL",
                0L,
                1_000L
        ));

        GameActionRequest request = createGameActionRequest();
        GameActionResponse response = gameActionService.saveGameAction(1L, request);

        assertEquals(2L, response.getActionLogId());
        assertEquals(1, response.getGameTick());
        assertEquals("BUY", response.getActionType());
        assertEquals("STOCK", response.getAssetType());
        assertEquals(2_000L, response.getTotalAssetPrincipal());
        assertEquals("NORMAL", response.getMarketState());
        assertEquals("NONE", response.getDepositStatus());
        assertEquals(2, actionLogMapper.getActionLogsByUserId(1L).size());
    }

    @Test
    @DisplayName("초기 자산 배분 로그가 없으면 게임 행동을 저장하지 않는다.")
    void saveGameActionRejectsGameThatHasNotStarted() {
        GameActionService gameActionService = createGameActionService(
                new InMemoryActionLogMapper()
        );

        assertThrows(
                IllegalStateException.class,
                () -> gameActionService.saveGameAction(1L, createGameActionRequest())
        );
    }

    @Test
    @DisplayName("게임 매도에는 증권 보유 기간 규칙을 적용하지 않는다.")
    void saveGameActionLogExcludesSecurityHoldingRules() {
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
        assertScoreEquals("0.00", sellActionLog.getLhScoreDelta());
        assertScoreEquals("0.00", sellActionLog.getRpScoreDelta());
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

    private GameActionRequest createGameActionRequest() {
        GameActionRequest request = new GameActionRequest();
        request.setGameTick(1);
        request.setActionType("BUY");
        request.setAssetType("STOCK");
        request.setActionAmount(1_000L);
        request.setCurrentCash(0L);
        request.setCurrentStockPrincipal(2_000L);
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

        @Override
        public int deleteActionLogsByUserId(Long userId) {
            int previousSize = actionLogs.size();
            actionLogs.removeIf(actionLog -> userId.equals(actionLog.getUserId()));
            return previousSize - actionLogs.size();
        }
    }
}
