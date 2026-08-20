package org.kkobi.assessment.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.domain.BehaviorAnalysisResult;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.domain.BehaviorEvent;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.enums.MarketState;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VirtualInvestmentImmediateRuleTest {

    private final BehaviorRuleEngine ruleEngine = new BehaviorRuleEngine();

    @Test
    @DisplayName("자산의 10% 미만 매수는 즉시 규칙에서 제외한다")
    void excludesSmallBuy() {
        BehaviorContext context = createBuyContext(MarketState.CRASH, 90L);

        BehaviorAnalysisResult result = calculate(context);

        assertEquals(0, result.getAppliedRules().size());
    }

    @Test
    @DisplayName("급락장 손실 종목 매수는 손실 대응 규칙을 우선 적용한다")
    void prioritizesLossAveragingOverCrashBuy() {
        BehaviorContext context = createBuyContext(MarketState.CRASH, 200L);
        context.getCurrentEvent().setPositionReturnRate(new BigDecimal("-20"));

        assertSingleRule(context, BehaviorRuleCode.LOSS_AVERAGING_BUY, "10", "-5", "0");
    }

    @Test
    @DisplayName("정상장에서 자산의 10~30%를 매수하면 계획 매수로 판정한다")
    void appliesNormalPlannedBuy() {
        BehaviorContext context = createBuyContext(MarketState.NORMAL, 200L);

        assertSingleRule(context, BehaviorRuleCode.NORMAL_PLANNED_BUY, "0", "-5", "5");
    }

    @Test
    @DisplayName("급등장에서 자산의 30% 이상을 매수하면 대규모 추세 매수 점수를 적용한다")
    void appliesLargeBullBuyScore() {
        BehaviorContext context = createBuyContext(MarketState.BULL, 300L);

        assertSingleRule(context, BehaviorRuleCode.BULL_BUY, "10", "-10", "5");
    }

    @Test
    @DisplayName("보유 수량의 20% 미만 매도는 즉시 규칙에서 제외한다")
    void excludesSmallSell() {
        BehaviorContext context = createSellContext(MarketState.NORMAL, 1, 9);
        context.getCurrentEvent().setRealizedReturnRate(new BigDecimal("-20"));

        BehaviorAnalysisResult result = calculate(context);

        assertEquals(0, result.getAppliedRules().size());
    }

    @Test
    @DisplayName("손실 구간에서 20~50%를 매도하면 부분 손절 점수를 적용한다")
    void appliesPartialLossCutScore() {
        BehaviorContext context = createSellContext(MarketState.NORMAL, 4, 6);
        context.getCurrentEvent().setRealizedReturnRate(new BigDecimal("-20"));

        assertSingleRule(context, BehaviorRuleCode.LOSS_CUT_SELL, "-5", "5", "-5");
    }

    @Test
    @DisplayName("급등장에서 보유 수량의 50% 이상을 수익 매도하면 유동성 점수를 높인다")
    void appliesLargeBullProfitSellScore() {
        BehaviorContext context = createSellContext(MarketState.BULL, 6, 4);
        context.getCurrentEvent().setRealizedReturnRate(new BigDecimal("10"));

        assertSingleRule(context, BehaviorRuleCode.BULL_PROFIT_SELL, "0", "10", "0");
    }

    private BehaviorContext createBuyContext(MarketState marketState, long actionAmount) {
        BehaviorEvent event = createSecurityEvent(BehaviorActionType.BUY);
        event.setActionAmount(actionAmount);
        event.setCurrentCash(500L);
        event.setCurrentStockPrincipal(400L);
        event.setCurrentDeposit(100L);

        BehaviorContext context = new BehaviorContext();
        context.setCurrentEvent(event);
        context.setMarketState(marketState);
        return context;
    }

    private BehaviorContext createSellContext(
            MarketState marketState,
            int sellQuantity,
            int currentQuantity) {
        BehaviorEvent event = createSecurityEvent(BehaviorActionType.SELL);
        event.setQuantity(sellQuantity);
        event.setCurrentSecurityQuantity(currentQuantity);

        BehaviorContext context = new BehaviorContext();
        context.setCurrentEvent(event);
        context.setMarketState(marketState);
        context.setFullSecuritySell(currentQuantity == 0);
        return context;
    }

    private BehaviorEvent createSecurityEvent(BehaviorActionType actionType) {
        BehaviorEvent event = new BehaviorEvent();
        event.setActionType(actionType);
        event.setAssetType(BehaviorAssetType.SECURITY);
        return event;
    }

    private BehaviorAnalysisResult calculate(BehaviorContext context) {
        return ruleEngine.calculateVirtualInvestmentBehaviorAnalysis(context);
    }

    private void assertSingleRule(
            BehaviorContext context,
            BehaviorRuleCode expectedRule,
            String expectedRt,
            String expectedLh,
            String expectedRp) {
        BehaviorAnalysisResult result = calculate(context);

        assertEquals(1, result.getAppliedRules().size());
        assertEquals(expectedRule, result.getAppliedRules().get(0).getRuleCode());
        assertScore(expectedRt, result.getTotalScoreDelta().getRtDelta());
        assertScore(expectedLh, result.getTotalScoreDelta().getLhDelta());
        assertScore(expectedRp, result.getTotalScoreDelta().getRpDelta());
    }

    private void assertScore(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
