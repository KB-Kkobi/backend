package org.kkobi.assessment.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.domain.BehaviorAnalysisResult;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.domain.BehaviorEvent;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.enums.PersonaType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorAnalysisFlowTest {

    private final BehaviorContextFactory behaviorContextFactory = new BehaviorContextFactory(
            new AssetRatioCalculator(),
            new MarketStateCalculator()
    );
    private final BehaviorRuleEngine behaviorRuleEngine = new BehaviorRuleEngine();
    private final GameScoreCalculator gameScoreCalculator = new GameScoreCalculator();
    private final PersonaClassifier personaClassifier = new PersonaClassifier();

    @Test
    @DisplayName("급락장에서 손실 종목을 추가 매수한 행동을 최종 성향까지 계산한다.")
    void calculateBehaviorAnalysisFlow() {
        BehaviorEvent currentEvent = createCrashBuyEvent();
        BehaviorContext behaviorContext = behaviorContextFactory.createBehaviorContext(
                currentEvent,
                List.of()
        );
        BehaviorAnalysisResult analysisResult = behaviorRuleEngine.calculateBehaviorAnalysis(
                behaviorContext
        );
        AssessmentScore assessmentScore = gameScoreCalculator.calculateGameScore(
                List.of(analysisResult.getTotalScoreDelta())
        );
        PersonaType personaType = personaClassifier.calculatePersona(assessmentScore);

        assertTrue(analysisResult.getAppliedRules().stream()
                .anyMatch(rule -> rule.getRuleCode() == BehaviorRuleCode.CRASH_BUY));
        assertTrue(analysisResult.getAppliedRules().stream()
                .anyMatch(rule -> rule.getRuleCode() == BehaviorRuleCode.LOSS_AVERAGING_BUY));
        assertEquals(0, new BigDecimal("25").compareTo(analysisResult.getTotalScoreDelta().getRtDelta()));
        assertEquals(0, new BigDecimal("-10").compareTo(analysisResult.getTotalScoreDelta().getLhDelta()));
        assertEquals(0, new BigDecimal("10").compareTo(analysisResult.getTotalScoreDelta().getRpDelta()));
        assertEquals(0, new BigDecimal("75.00").compareTo(assessmentScore.getRtScore()));
        assertEquals(0, new BigDecimal("40.00").compareTo(assessmentScore.getLhScore()));
        assertEquals(0, new BigDecimal("60.00").compareTo(assessmentScore.getRpScore()));
        assertEquals(PersonaType.HLH, personaType);
    }

    private BehaviorEvent createCrashBuyEvent() {
        BehaviorEvent event = new BehaviorEvent();
        event.setActionType(BehaviorActionType.BUY);
        event.setAssetType(BehaviorAssetType.SECURITY);
        event.setSecurityId(1L);
        event.setActionAmount(100_000L);
        event.setCurrentCash(100_000L);
        event.setCurrentStockPrincipal(800_000L);
        event.setCurrentDeposit(100_000L);
        event.setCurrentPriceChangeRate(new BigDecimal("-10"));
        event.setPositionReturnRate(new BigDecimal("-20"));
        event.setTradedAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        return event;
    }
}
