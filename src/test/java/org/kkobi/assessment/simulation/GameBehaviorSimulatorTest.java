package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.calculator.PersonaClassifier;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.service.ScenarioService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameBehaviorSimulatorTest {

    private static final long RANDOM_SEED = 20260815L;

    private final ScenarioService scenarioService = new ScenarioService();
    private final GameBehaviorSimulator gameBehaviorSimulator =
            new GameBehaviorSimulator();

    @Test
    @DisplayName("생성된 게임 행동을 기존 성향 계산 엔진으로 분석한다.")
    void simulateGameBehaviorAssessment() {
        ScenarioDto scenario = scenarioService.getScenario("SC001");
        SimulatedGamePortfolio initialPortfolio = createInitialPortfolio();

        GameBehaviorSimulationResult result = gameBehaviorSimulator.simulateGame(
                1L,
                scenario,
                initialPortfolio,
                RANDOM_SEED
        );

        assertEquals(BigDecimal.valueOf(20).setScale(2), result.getInitialCashRatio());
        assertEquals(BigDecimal.valueOf(70).setScale(2), result.getInitialStockRatio());
        assertEquals(BigDecimal.valueOf(10).setScale(2), result.getInitialDepositRatio());
        assertEquals(
                1,
                result.getRuleApplicationCounts().get(BehaviorRuleCode.INITIAL_STOCK_ALLOCATION)
        );
        assertTrue(result.getTradeCount() > 0);
        assertTrue(result.getRuleApplicationCounts().values()
                .stream()
                .mapToInt(Integer::intValue)
                .sum() > 0);
        assertTrue(result.isDepositCancelled() ^ result.isDepositMatured());
        assertScoreRange(result.getFinalRtScore());
        assertScoreRange(result.getFinalLhScore());
        assertScoreRange(result.getFinalRpScore());
        assertEquals(
                new PersonaClassifier().calculatePersona(new AssessmentScore(
                        result.getFinalRtScore(),
                        result.getFinalLhScore(),
                        result.getFinalRpScore()
                )),
                result.getPersonaType()
        );
    }

    @Test
    @DisplayName("동일한 Seed는 동일한 최종 점수와 성향을 생성한다.")
    void simulateSameAssessmentWithSameRandomSeed() {
        ScenarioDto scenario = scenarioService.getScenario("SC001");

        GameBehaviorSimulationResult firstResult = gameBehaviorSimulator.simulateGame(
                1L,
                scenario,
                createInitialPortfolio(),
                RANDOM_SEED
        );
        GameBehaviorSimulationResult secondResult = gameBehaviorSimulator.simulateGame(
                2L,
                scenario,
                createInitialPortfolio(),
                RANDOM_SEED
        );

        assertEquals(firstResult.getFinalRtScore(), secondResult.getFinalRtScore());
        assertEquals(firstResult.getFinalLhScore(), secondResult.getFinalLhScore());
        assertEquals(firstResult.getFinalRpScore(), secondResult.getFinalRpScore());
        assertEquals(firstResult.getPersonaType(), secondResult.getPersonaType());
        assertEquals(
                firstResult.getRuleApplicationCounts(),
                secondResult.getRuleApplicationCounts()
        );
    }

    @Test
    @DisplayName("시뮬레이션은 전달받은 초기 자산 객체를 변경하지 않는다.")
    void protectInitialPortfolio() {
        SimulatedGamePortfolio initialPortfolio = createInitialPortfolio();

        gameBehaviorSimulator.simulateGame(
                1L,
                scenarioService.getScenario("SC001"),
                initialPortfolio,
                RANDOM_SEED
        );

        assertEquals(2_000_000L, initialPortfolio.getCurrentCash());
        assertEquals(7_000_000L, initialPortfolio.getCurrentStockPrincipal());
        assertEquals(1_000_000L, initialPortfolio.getCurrentDeposit());
        assertEquals(350, initialPortfolio.getCurrentStockQuantity());
        assertFalse(initialPortfolio.isDepositCancelled());
        assertFalse(initialPortfolio.isDepositMatured());
    }

    private SimulatedGamePortfolio createInitialPortfolio() {
        return new SimulatedGamePortfolio(
                2_000_000L,
                7_000_000L,
                1_000_000L,
                350
        );
    }

    private void assertScoreRange(BigDecimal score) {
        assertTrue(score.compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(score.compareTo(BigDecimal.valueOf(100)) <= 0);
    }
}
