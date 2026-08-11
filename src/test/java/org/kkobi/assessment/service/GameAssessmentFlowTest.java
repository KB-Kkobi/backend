package org.kkobi.assessment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.calculator.AssetRatioCalculator;
import org.kkobi.assessment.calculator.BehaviorContextFactory;
import org.kkobi.assessment.calculator.BehaviorRuleEngine;
import org.kkobi.assessment.calculator.GameScoreCalculator;
import org.kkobi.assessment.calculator.MarketStateCalculator;
import org.kkobi.assessment.calculator.PersonaClassifier;
import org.kkobi.assessment.calculator.SecurityPriceRateCalculator;
import org.kkobi.assessment.domain.AssessmentResult;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.assessment.mapper.AssessmentMapper;
import org.kkobi.game.calculator.GamePriceRateCalculator;
import org.kkobi.game.calculator.GameSecurityReturnCalculator;
import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.dto.GameBehaviorRequest;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.dto.ScenarioTickDto;
import org.kkobi.game.mapper.ActionLogMapper;
import org.kkobi.game.service.ActionLogService;
import org.kkobi.game.service.GameActionService;
import org.kkobi.game.service.ScenarioService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameAssessmentFlowTest {

    @Test
    @DisplayName("초기 배분과 게임 행동을 저장한 뒤 최종 점수와 성향 유형을 계산한다.")
    void calculateGameAssessmentFromSavedActionLogs() {
        InMemoryActionLogMapper actionLogMapper = new InMemoryActionLogMapper();
        ActionLogService actionLogService = new ActionLogService(actionLogMapper);
        BehaviorRuleEngine behaviorRuleEngine = new BehaviorRuleEngine();
        GameActionService gameActionService = createGameActionService(
                actionLogService,
                behaviorRuleEngine
        );
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        GameAssessmentService gameAssessmentService = new GameAssessmentService(
                actionLogService,
                behaviorRuleEngine,
                new GameScoreCalculator(),
                new AssessmentResultService(
                        assessmentMapper,
                        new PersonaClassifier()
                )
        );

        gameActionService.saveGameActionLog(createGameBehaviorRequest(
                0,
                "INITIAL_ALLOCATION",
                "ALL",
                0L,
                500_000L,
                500_000L,
                1_000_000L
        ));
        gameActionService.saveGameActionLog(createGameBehaviorRequest(
                1,
                "DEPOSIT_CANCEL",
                "DEPOSIT",
                500_000L,
                500_000L,
                0L,
                500_000L
        ));
        gameActionService.saveGameActionLog(createGameBehaviorRequest(
                1,
                "BUY",
                "STOCK",
                400_000L,
                600_000L,
                0L,
                100_000L
        ));

        AssessmentResult result = gameAssessmentService.calculateGameAssessment(1L);
        List<ActionLogDto> actionLogs = actionLogMapper.getActionLogsByUserId(1L);

        assertEquals(3, actionLogs.size());
        assertScoreEquals("-10", actionLogs.get(0).getRtScoreDelta());
        assertScoreEquals("-5", actionLogs.get(0).getLhScoreDelta());
        assertScoreEquals("-5", actionLogs.get(0).getRpScoreDelta());
        assertScoreEquals("5", actionLogs.get(2).getRtScoreDelta());
        assertScoreEquals("-10", actionLogs.get(2).getLhScoreDelta());
        assertScoreEquals("10", actionLogs.get(2).getRpScoreDelta());
        assertScoreEquals("45.00", result.getAssessmentScore().getRtScore());
        assertScoreEquals("35.00", result.getAssessmentScore().getLhScore());
        assertScoreEquals("55.00", result.getAssessmentScore().getRpScore());
        assertEquals(PersonaType.LLH, result.getPersonaType());
        assertEquals(1, assessmentMapper.getSavedResultCount());
        assertEquals(result.getAssessmentScore(), assessmentMapper.getSavedAssessmentScore());
    }

    private GameActionService createGameActionService(
            ActionLogService actionLogService,
            BehaviorRuleEngine behaviorRuleEngine) {
        MarketStateCalculator marketStateCalculator = new MarketStateCalculator();
        return new GameActionService(
                actionLogService,
                createScenarioService(),
                new GamePriceRateCalculator(new SecurityPriceRateCalculator()),
                new GameSecurityReturnCalculator(),
                new BehaviorContextFactory(
                        new AssetRatioCalculator(),
                        marketStateCalculator
                ),
                behaviorRuleEngine
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
                        createScenarioTick(1, "2026-01-08")
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
            long currentStock,
            long currentDeposit,
            long actionAmount) {
        GameBehaviorRequest request = new GameBehaviorRequest();
        request.setUserId(1L);
        request.setScenarioId("TEST");
        request.setTick(tick);
        request.setActionType(actionType);
        request.setAssetType(assetType);
        request.setActionAmount(actionAmount);
        request.setCurrentCash(currentCash);
        request.setCurrentStock(currentStock);
        request.setCurrentDeposit(currentDeposit);
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

    private static class InMemoryAssessmentMapper implements AssessmentMapper {

        private AssessmentScore savedAssessmentScore;
        private int savedResultCount;

        @Override
        public AssessmentScore getLatestAssessmentScore(Long userId) {
            return savedAssessmentScore;
        }

        @Override
        public org.kkobi.assessment.domain.AssessmentResultDetails
                getLatestAssessmentResultDetails(Long userId) {
            return null;
        }

        @Override
        public org.kkobi.assessment.dto.AssessmentResultResponseDto
                getLatestAssessmentResult(Long userId) {
            return null;
        }

        @Override
        public Long getPersonaIdByAxisCode(String axisCode) {
            return 7L;
        }

        @Override
        public int saveAssessmentResult(
                Long userId,
                Long personaId,
                AssessmentScore assessmentScore) {
            savedAssessmentScore = assessmentScore;
            savedResultCount++;
            return 1;
        }

        private AssessmentScore getSavedAssessmentScore() {
            return savedAssessmentScore;
        }

        private int getSavedResultCount() {
            return savedResultCount;
        }
    }
}
