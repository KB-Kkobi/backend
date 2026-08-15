package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.enums.PersonaType;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.service.ScenarioService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameBehaviorSimulationAnalyzerTest {

    private static final int SIMULATION_COUNT = 200;
    private static final long RANDOM_SEED = 20260815L;

    private final ScenarioService scenarioService = new ScenarioService();
    private final GameBehaviorSimulationAnalyzer simulationAnalyzer =
            new GameBehaviorSimulationAnalyzer();

    @Test
    @DisplayName("여러 가상 사용자의 성향 분포와 점수 통계를 계산한다.")
    void analyzeGameSimulations() {
        GameBehaviorSimulationAnalysis analysis = simulationAnalyzer.analyzeGameSimulations(
                scenarioService.getScenario("SC001"),
                SIMULATION_COUNT,
                RANDOM_SEED
        );

        assertEquals(SIMULATION_COUNT, analysis.getTotalSimulationCount());
        assertEquals(PersonaType.values().length, analysis.getPersonaSummaries().size());
        assertEquals(
                SIMULATION_COUNT,
                analysis.getPersonaSummaries().values()
                        .stream()
                        .mapToInt(GameBehaviorSimulationAnalysis.PersonaSummary::getSimulationCount)
                        .sum()
        );
        assertDistributionRate(analysis);
        assertScoreSummary(analysis.getOverallRtScoreSummary());
        assertScoreSummary(analysis.getOverallLhScoreSummary());
        assertScoreSummary(analysis.getOverallRpScoreSummary());
        assertBehaviorStatistics(analysis.getBehaviorStatistics());
        assertEquals(BehaviorRuleCode.values().length, analysis.getRuleStatistics().size());
        assertScoreDiagnostic(analysis.getRtScoreDiagnostic());
        assertScoreDiagnostic(analysis.getLhScoreDiagnostic());
        assertScoreDiagnostic(analysis.getRpScoreDiagnostic());
    }

    @Test
    @DisplayName("판정 사용자가 없는 성향도 0건 통계로 포함한다.")
    void includeAllPersonaSummaries() {
        GameBehaviorSimulationAnalysis analysis = simulationAnalyzer.analyzeGameSimulations(
                scenarioService.getScenario("SC001"),
                1,
                RANDOM_SEED
        );

        long emptyPersonaCount = analysis.getPersonaSummaries().values()
                .stream()
                .filter(summary -> summary.getSimulationCount() == 0)
                .count();

        assertEquals(7, emptyPersonaCount);
    }

    @Test
    @DisplayName("동일한 Seed는 동일한 성향 분포와 평균을 계산한다.")
    void analyzeSameDistributionWithSameRandomSeed() {
        ScenarioDto scenario = scenarioService.getScenario("SC001");

        GameBehaviorSimulationAnalysis firstAnalysis = simulationAnalyzer.analyzeGameSimulations(
                scenario,
                50,
                RANDOM_SEED
        );
        GameBehaviorSimulationAnalysis secondAnalysis = simulationAnalyzer.analyzeGameSimulations(
                scenario,
                50,
                RANDOM_SEED
        );

        for (PersonaType personaType : PersonaType.values()) {
            assertEquals(
                    firstAnalysis.getPersonaSummary(personaType).getSimulationCount(),
                    secondAnalysis.getPersonaSummary(personaType).getSimulationCount()
            );
        }
        assertEquals(
                firstAnalysis.getOverallRtScoreSummary().getAverage(),
                secondAnalysis.getOverallRtScoreSummary().getAverage()
        );
        assertEquals(
                firstAnalysis.getOverallLhScoreSummary().getStandardDeviation(),
                secondAnalysis.getOverallLhScoreSummary().getStandardDeviation()
        );
    }

    @Test
    @DisplayName("시뮬레이션 수는 0보다 커야 한다.")
    void rejectInvalidSimulationCount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> simulationAnalyzer.analyzeGameSimulations(
                        scenarioService.getScenario("SC001"),
                        0,
                        RANDOM_SEED
                )
        );
    }

    private void assertDistributionRate(GameBehaviorSimulationAnalysis analysis) {
        BigDecimal totalDistributionRate = analysis.getPersonaSummaries().values()
                .stream()
                .map(GameBehaviorSimulationAnalysis.PersonaSummary::getDistributionRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertTrue(totalDistributionRate.subtract(BigDecimal.valueOf(100)).abs()
                .compareTo(BigDecimal.valueOf(0.05)) <= 0);
    }

    private void assertScoreSummary(GameBehaviorSimulationAnalysis.ScoreSummary scoreSummary) {
        assertTrue(scoreSummary.getAverage().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(scoreSummary.getAverage().compareTo(BigDecimal.valueOf(100)) <= 0);
        assertTrue(scoreSummary.getStandardDeviation().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(scoreSummary.getMinimum().compareTo(scoreSummary.getMaximum()) <= 0);
    }

    private void assertBehaviorStatistics(
            GameBehaviorSimulationAnalysis.BehaviorStatistics behaviorStatistics) {
        assertTrue(behaviorStatistics.getAverageBuyCount().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(behaviorStatistics.getAverageSellCount().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(behaviorStatistics.getAverageNoActionTickCount()
                .compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(behaviorStatistics.getAverageActionCountPerTick()
                .compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(behaviorStatistics.getConsecutiveActionLevelTwoCount() >= 0);
        assertTrue(behaviorStatistics.getConsecutiveActionLevelThreeOrMoreCount() >= 0);
    }

    private void assertScoreDiagnostic(
            GameBehaviorSimulationAnalysis.ScoreDiagnostic scoreDiagnostic) {
        assertTrue(scoreDiagnostic.getMaximumScoreCount() >= 0);
        assertTrue(scoreDiagnostic.getMaximumScoreRate().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(scoreDiagnostic.getMaximumScoreRate()
                .compareTo(BigDecimal.valueOf(100)) <= 0);
        assertTrue(scoreDiagnostic.getBoundaryScoreCount() >= 0);
        assertTrue(scoreDiagnostic.getBoundaryScoreRate().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(scoreDiagnostic.getBoundaryScoreRate()
                .compareTo(BigDecimal.valueOf(100)) <= 0);
    }
}
