package org.kkobi.assessment.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.domain.BehaviorAnalysisResult;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.domain.BehaviorEvent;
import org.kkobi.assessment.domain.ScoreDelta;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;
import org.kkobi.assessment.enums.MarketState;
import org.kkobi.assessment.enums.PersonaType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersonaBehaviorReachabilityTest {

    private final BehaviorRuleEngine behaviorRuleEngine = new BehaviorRuleEngine();
    private final GameScoreCalculator gameScoreCalculator = new GameScoreCalculator();
    private final PersonaClassifier personaClassifier = new PersonaClassifier();

    @Test
    @DisplayName("대표 투자 행동 조합으로 8가지 성향에 모두 도달한다.")
    void reachAllPersonasWithRepresentativeBehaviors() {
        Set<PersonaType> reachedPersonas = EnumSet.noneOf(PersonaType.class);

        reachedPersonas.add(assertPersona(PersonaType.HHH, List.of(
                createInitialAllocationContext(70, 20, 10),
                createCrashBuyContext(),
                createLossCutContext(),
                createLossCutContext()
        )));
        reachedPersonas.add(assertPersona(PersonaType.HHL, List.of(
                createInitialAllocationContext(70, 30, 0),
                createCrashBuyContext(),
                createLossCutContext(),
                createLossCutContext()
        )));
        reachedPersonas.add(assertPersona(PersonaType.HLH, List.of(
                createInitialAllocationContext(70, 20, 10),
                createCrashBuyContext()
        )));
        reachedPersonas.add(assertPersona(PersonaType.HLL, List.of(
                createInitialAllocationContext(70, 20, 10),
                createCrashBuyContext(),
                createLossCutContext(),
                createLossCutContext(),
                createDepositMaturityContext()
        )));
        reachedPersonas.add(assertPersona(PersonaType.LHH, List.of(
                createInitialAllocationContext(40, 30, 30),
                createBullProfitSellContext()
        )));
        reachedPersonas.add(assertPersona(PersonaType.LHL, List.of(
                createInitialAllocationContext(40, 30, 30)
        )));
        reachedPersonas.add(assertPersona(PersonaType.LLH, List.of(
                createInitialAllocationContext(30, 20, 50),
                createBullBuyContext()
        )));
        reachedPersonas.add(assertPersona(PersonaType.LLL, List.of(
                createInitialAllocationContext(30, 20, 50),
                createDepositMaturityContext()
        )));

        assertEquals(EnumSet.allOf(PersonaType.class), reachedPersonas);
    }

    @Test
    @DisplayName("주식 비중 확대 후 손절과 예금 만기 유지는 HLL 성향으로 판정한다.")
    void reachHllWithRiskTakingAndConservativeBehaviors() {
        AssessmentScore assessmentScore = calculateScore(List.of(
                createInitialAllocationContext(70, 20, 10),
                createCrashBuyContext(),
                createLossCutContext(),
                createLossCutContext(),
                createDepositMaturityContext()
        ));

        assertScore(assessmentScore, "55.00", "40.00", "45.00");
        assertEquals(PersonaType.HLL, personaClassifier.calculatePersona(assessmentScore));
    }

    @Test
    @DisplayName("주식과 현금을 함께 확보하고 손절한 행동은 HHL 성향으로 판정한다.")
    void reachHhlWithRiskTakingAndLiquidityBehaviors() {
        AssessmentScore assessmentScore = calculateScore(List.of(
                createInitialAllocationContext(70, 30, 0),
                createCrashBuyContext(),
                createLossCutContext(),
                createLossCutContext()
        ));

        assertScore(assessmentScore, "55.00", "60.00", "45.00");
        assertEquals(PersonaType.HHL, personaClassifier.calculatePersona(assessmentScore));
    }

    @Test
    @DisplayName("예금 비중을 높이고 만기까지 유지한 행동은 LLL 성향으로 판정한다.")
    void reachLllWithDepositMaturityBehavior() {
        AssessmentScore assessmentScore = calculateScore(List.of(
                createInitialAllocationContext(30, 20, 50),
                createDepositMaturityContext()
        ));

        assertScore(assessmentScore, "35.00", "35.00", "40.00");
        assertEquals(PersonaType.LLL, personaClassifier.calculatePersona(assessmentScore));
    }

    private PersonaType calculatePersona(List<BehaviorContext> behaviorContexts) {
        return personaClassifier.calculatePersona(calculateScore(behaviorContexts));
    }

    private PersonaType assertPersona(
            PersonaType expectedPersona,
            List<BehaviorContext> behaviorContexts) {
        PersonaType personaType = calculatePersona(behaviorContexts);
        assertEquals(expectedPersona, personaType);
        return personaType;
    }

    private AssessmentScore calculateScore(List<BehaviorContext> behaviorContexts) {
        List<ScoreDelta> scoreDeltas = new ArrayList<>();

        for (BehaviorContext behaviorContext : behaviorContexts) {
            BehaviorAnalysisResult analysisResult = behaviorRuleEngine
                    .calculateGameBehaviorAnalysis(behaviorContext);
            scoreDeltas.add(analysisResult.getTotalScoreDelta());
        }

        return gameScoreCalculator.calculateGameScore(scoreDeltas);
    }

    private BehaviorContext createInitialAllocationContext(
            int stockRatio,
            int cashRatio,
            int depositRatio) {
        BehaviorContext behaviorContext = new BehaviorContext();
        behaviorContext.setInitialAllocation(true);
        behaviorContext.setStockRatio(BigDecimal.valueOf(stockRatio));
        behaviorContext.setCashRatio(BigDecimal.valueOf(cashRatio));
        behaviorContext.setDepositRatio(BigDecimal.valueOf(depositRatio));
        return behaviorContext;
    }

    private BehaviorContext createCrashBuyContext() {
        BehaviorEvent behaviorEvent = createSecurityEvent(BehaviorActionType.BUY);
        behaviorEvent.setPositionReturnRate(new BigDecimal("-5"));

        BehaviorContext behaviorContext = new BehaviorContext();
        behaviorContext.setCurrentEvent(behaviorEvent);
        behaviorContext.setMarketState(MarketState.CRASH);
        return behaviorContext;
    }

    private BehaviorContext createLossCutContext() {
        BehaviorEvent behaviorEvent = createSecurityEvent(BehaviorActionType.SELL);
        behaviorEvent.setRealizedReturnRate(new BigDecimal("-10"));

        BehaviorContext behaviorContext = new BehaviorContext();
        behaviorContext.setCurrentEvent(behaviorEvent);
        behaviorContext.setMarketState(MarketState.NORMAL);
        return behaviorContext;
    }

    private BehaviorContext createBullProfitSellContext() {
        BehaviorEvent behaviorEvent = createSecurityEvent(BehaviorActionType.SELL);
        behaviorEvent.setRealizedReturnRate(new BigDecimal("10"));

        BehaviorContext behaviorContext = new BehaviorContext();
        behaviorContext.setCurrentEvent(behaviorEvent);
        behaviorContext.setMarketState(MarketState.BULL);
        return behaviorContext;
    }

    private BehaviorContext createBullBuyContext() {
        BehaviorEvent behaviorEvent = createSecurityEvent(BehaviorActionType.BUY);
        behaviorEvent.setPositionReturnRate(new BigDecimal("-5"));

        BehaviorContext behaviorContext = new BehaviorContext();
        behaviorContext.setCurrentEvent(behaviorEvent);
        behaviorContext.setMarketState(MarketState.BULL);
        return behaviorContext;
    }

    private BehaviorContext createDepositMaturityContext() {
        BehaviorContext behaviorContext = new BehaviorContext();
        behaviorContext.setDepositMatured(true);
        return behaviorContext;
    }

    private BehaviorEvent createSecurityEvent(BehaviorActionType actionType) {
        BehaviorEvent behaviorEvent = new BehaviorEvent();
        behaviorEvent.setActionType(actionType);
        behaviorEvent.setAssetType(BehaviorAssetType.SECURITY);
        return behaviorEvent;
    }

    private void assertScore(
            AssessmentScore assessmentScore,
            String expectedRtScore,
            String expectedLhScore,
            String expectedRpScore) {
        assertEquals(0, new BigDecimal(expectedRtScore).compareTo(assessmentScore.getRtScore()));
        assertEquals(0, new BigDecimal(expectedLhScore).compareTo(assessmentScore.getLhScore()));
        assertEquals(0, new BigDecimal(expectedRpScore).compareTo(assessmentScore.getRpScore()));
    }
}
