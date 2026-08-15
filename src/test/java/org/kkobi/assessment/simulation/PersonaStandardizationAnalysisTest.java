package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.enums.PersonaType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonaStandardizationAnalysisTest {

    @Test
    @DisplayName("유형별 평균과 표준편차 및 사용자별 표준화 결과를 계산한다.")
    void calculateStandardizationStatistics() {
        List<PersonaProfileSimulationResult> results = createResults(false);

        PersonaStandardizationAnalysis analysis =
                new PersonaStandardizationAnalysis(results);

        assertEquals(BigDecimal.valueOf(55).setScale(4),
                analysis.getStatistics(PersonaType.HHH).rtAverage());
        assertEquals(BigDecimal.valueOf(5).setScale(4),
                analysis.getStatistics(PersonaType.HHH).rtStandardDeviation());
        assertEquals(16, analysis.getStandardizedResults().size());
        assertTrue(analysis.getStandardizedResults().stream()
                .allMatch(result -> result.similarityPercentile().signum() > 0));
    }

    @Test
    @DisplayName("표준편차가 0이면 Z-score를 0으로 계산한다.")
    void returnZeroZScoreWhenStandardDeviationIsZero() {
        PersonaStandardizationAnalysis analysis =
                new PersonaStandardizationAnalysis(createResults(true));

        assertTrue(analysis.getStandardizedResults().stream()
                .allMatch(result -> result.rtZScore().signum() == 0));
    }

    private List<PersonaProfileSimulationResult> createResults(boolean sameScore) {
        List<PersonaProfileSimulationResult> results = new ArrayList<>();
        long userId = 1;
        for (PersonaType personaType : PersonaType.values()) {
            results.add(createResult(userId++, personaType, BigDecimal.valueOf(50)));
            results.add(createResult(
                    userId++,
                    personaType,
                    sameScore ? BigDecimal.valueOf(50) : BigDecimal.valueOf(60)
            ));
        }
        return results;
    }

    private PersonaProfileSimulationResult createResult(
            long userId,
            PersonaType personaType,
            BigDecimal score) {
        GameBehaviorSimulationResult simulationResult = GameBehaviorSimulationResult.builder()
                .simulationUserId(userId)
                .initialCashRatio(BigDecimal.valueOf(30))
                .initialStockRatio(BigDecimal.valueOf(40))
                .initialDepositRatio(BigDecimal.valueOf(30))
                .ruleApplicationCounts(Map.of())
                .ruleScoreContributions(Map.of())
                .finalRtScore(score)
                .finalLhScore(score)
                .finalRpScore(score)
                .personaType(personaType)
                .build();
        return new PersonaProfileSimulationResult(1L, personaType, simulationResult);
    }
}
