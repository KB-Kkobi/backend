package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.Test;
import org.kkobi.assessment.domain.ScoreDelta;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.enums.PersonaType;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonaRuleApplicationAnalysisTest {

    @Test
    void calculateRuleApplicationAndContributionByPersona() {
        PersonaRuleApplicationAnalysis analysis = new PersonaRuleApplicationAnalysis();
        analysis.addResult(createResult(2, new ScoreDelta(
                BigDecimal.TEN,
                BigDecimal.valueOf(-10),
                BigDecimal.valueOf(20)
        )));
        analysis.addResult(createResult(0, null));

        PersonaRuleApplicationAnalysis.RuleSummary summary = analysis
                .getRuleSummaries(PersonaType.HLH)
                .stream()
                .filter(ruleSummary -> ruleSummary.ruleCode() == BehaviorRuleCode.BULL_BUY)
                .findFirst()
                .orElseThrow();

        assertEquals(2, analysis.getPersonaUserCount(PersonaType.HLH));
        assertEquals(2, summary.applicationCount());
        assertEquals(1, summary.appliedUserCount());
        assertEquals(new BigDecimal("50.00"), summary.appliedUserRate());
        assertEquals(new BigDecimal("1.0000"), summary.averageApplicationCountPerUser());
        assertEquals(new BigDecimal("5.0000"), summary.rtAverageContributionPerUser());
        assertEquals(new BigDecimal("-5.0000"), summary.lhAverageContributionPerUser());
        assertEquals(new BigDecimal("10.0000"), summary.rpAverageContributionPerUser());
    }

    private GameBehaviorSimulationResult createResult(
            int bullBuyCount,
            ScoreDelta bullBuyContribution) {
        return GameBehaviorSimulationResult.builder()
                .simulationUserId(bullBuyCount + 1L)
                .initialCashRatio(BigDecimal.valueOf(30))
                .initialStockRatio(BigDecimal.valueOf(40))
                .initialDepositRatio(BigDecimal.valueOf(30))
                .buyCount(0)
                .sellCount(0)
                .noActionTickCount(52)
                .totalBuyAmount(0)
                .totalSellAmount(0)
                .fullSellCount(0)
                .crashBuyCount(0)
                .crashFullSellCount(0)
                .bullBuyCount(bullBuyCount)
                .bullProfitSellCount(0)
                .lossAveragingBuyCount(0)
                .lossCutSellCount(0)
                .depositCancelled(false)
                .depositMatured(false)
                .boughtStockAfterDepositCancel(false)
                .maximumConsecutiveBuyCount(0)
                .maximumConsecutiveSellCount(0)
                .consecutiveActionLevelTwoCount(0)
                .consecutiveActionLevelThreeOrMoreCount(0)
                .ruleApplicationCounts(bullBuyCount == 0
                        ? Map.of()
                        : Map.of(BehaviorRuleCode.BULL_BUY, bullBuyCount))
                .ruleScoreContributions(bullBuyContribution == null
                        ? Map.of()
                        : Map.of(BehaviorRuleCode.BULL_BUY, bullBuyContribution))
                .finalRtScore(BigDecimal.valueOf(60))
                .finalLhScore(BigDecimal.valueOf(40))
                .finalRpScore(BigDecimal.valueOf(60))
                .personaType(PersonaType.HLH)
                .build();
    }
}
