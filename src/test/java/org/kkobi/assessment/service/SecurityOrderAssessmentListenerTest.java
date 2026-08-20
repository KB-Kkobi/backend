package org.kkobi.assessment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.calculator.AssetRatioCalculator;
import org.kkobi.assessment.calculator.BehaviorContextFactory;
import org.kkobi.assessment.calculator.BehaviorRuleEngine;
import org.kkobi.assessment.calculator.MarketStateCalculator;
import org.kkobi.assessment.calculator.PersonaClassifier;
import org.kkobi.assessment.calculator.SecurityPositionCalculator;
import org.kkobi.assessment.calculator.SecurityPriceRateCalculator;
import org.kkobi.assessment.calculator.VirtualInvestmentScoreCalculator;
import org.kkobi.assessment.domain.AssessmentResult;
import org.kkobi.assessment.domain.AssessmentResultDetails;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.dto.AssessmentResultResponseDto;
import org.kkobi.assessment.dto.VirtualInvestmentBehaviorDto;
import org.kkobi.assessment.dto.VirtualInvestmentBehaviorRequest;
import org.kkobi.assessment.mapper.AssessmentMapper;
import org.kkobi.assessment.mapper.VirtualInvestmentBehaviorMapper;
import org.kkobi.assessment.validator.VirtualInvestmentBehaviorValidator;
import org.kkobi.product.holding.dto.PreferentialRateConditionInfoDto;
import org.kkobi.product.holding.dto.ProductHoldingCreateDto;
import org.kkobi.product.holding.dto.ProductHoldingInfoDto;
import org.kkobi.product.holding.dto.ProductSubscriptionInfoDto;
import org.kkobi.product.holding.dto.response.ProductHoldingTransactionHistoryResponseDto;
import org.kkobi.product.mapper.ProductHoldingMapper;
import org.kkobi.trade.dto.HoldingDto;
import org.kkobi.trade.dto.TradeAccountDto;
import org.kkobi.trade.event.SecurityOrderFilledEvent;
import org.kkobi.trade.mapper.HoldingMapper;
import org.kkobi.trade.mapper.TradeAccountMapper;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityOrderAssessmentListenerTest {

    @Test
    @DisplayName("급락장 매수는 CRASH_BUY 규칙을 적용해 점수를 저장한다.")
    void crashBuyAppliesCrashBuyRule() {
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        SecurityOrderAssessmentListener listener = createListener(
                800L, 200L, 0L, List.of(), assessmentMapper
        );

        listener.onSecurityOrderFilled(createBuyEvent(new BigDecimal("-5.50"), 2, 200L));

        assertEquals(1, assessmentMapper.getSavedResultCount());
        assertScoreEquals("53.33", assessmentMapper.getSavedAssessmentScore().getRtScore());
        assertScoreEquals("48.34", assessmentMapper.getSavedAssessmentScore().getLhScore());
        assertScoreEquals("50.00", assessmentMapper.getSavedAssessmentScore().getRpScore());
    }

    @Test
    @DisplayName("급등장 매수는 BULL_BUY 규칙을 적용해 점수를 저장한다.")
    void bullBuyAppliesBullBuyRule() {
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        SecurityOrderAssessmentListener listener = createListener(
                800L, 200L, 0L, List.of(), assessmentMapper
        );

        listener.onSecurityOrderFilled(createBuyEvent(new BigDecimal("3.50"), 2, 200L));

        assertEquals(1, assessmentMapper.getSavedResultCount());
        assertScoreEquals("50.00", assessmentMapper.getSavedAssessmentScore().getRtScore());
        assertScoreEquals("48.34", assessmentMapper.getSavedAssessmentScore().getLhScore());
        assertScoreEquals("53.33", assessmentMapper.getSavedAssessmentScore().getRpScore());
    }

    @Test
    @DisplayName("급등장 수익 실현 매도는 BULL_PROFIT_SELL 규칙을 적용해 점수를 저장한다.")
    void bullProfitSellAppliesBullProfitSellRule() {
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        // 이전 매수 평균단가=80, 현재 매도 qty=5 amt=500 → 체결단가=100 → 실현손익=+25%
        SecurityOrderAssessmentListener listener = createListener(
                500L, 400L, 0L, List.of(createPreviousBuy(80L, 10)), assessmentMapper
        );

        listener.onSecurityOrderFilled(createSellEvent(new BigDecimal("3.50"), 5, 500L));

        assertEquals(1, assessmentMapper.getSavedResultCount());
        assertScoreEquals("50.00", assessmentMapper.getSavedAssessmentScore().getRtScore());
        assertScoreEquals("53.33", assessmentMapper.getSavedAssessmentScore().getLhScore());
        assertScoreEquals("50.00", assessmentMapper.getSavedAssessmentScore().getRpScore());
    }

    @Test
    @DisplayName("손절매는 LOSS_CUT_SELL 규칙을 적용해 점수를 저장한다.")
    void lossCutSellAppliesLossCutSellRule() {
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        // 이전 매수 평균단가=100, 현재 매도 qty=5 amt=440 → 체결단가=88 → 실현손익=-12%
        SecurityOrderAssessmentListener listener = createListener(
                440L, 500L, 0L, List.of(createPreviousBuy(100L, 10)), assessmentMapper
        );

        listener.onSecurityOrderFilled(createSellEvent(BigDecimal.ZERO, 5, 440L));

        assertEquals(1, assessmentMapper.getSavedResultCount());
        assertScoreEquals("46.67", assessmentMapper.getSavedAssessmentScore().getRtScore());
        assertScoreEquals("53.33", assessmentMapper.getSavedAssessmentScore().getLhScore());
        assertScoreEquals("48.34", assessmentMapper.getSavedAssessmentScore().getRpScore());
    }

    @Test
    @DisplayName("물타기 매수는 LOSS_AVERAGING_BUY 규칙을 적용해 점수를 저장한다.")
    void lossAveragingBuyAppliesLossAveragingBuyRule() {
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        // 이전 매수 평균단가=100, 현재 매수 qty=5 amt=400 → 체결단가=80 → 포지션손익=-20%
        SecurityOrderAssessmentListener listener = createListener(
                600L, 800L, 0L, List.of(createPreviousBuy(100L, 10)), assessmentMapper
        );

        listener.onSecurityOrderFilled(createBuyEvent(BigDecimal.ZERO, 5, 400L));

        assertEquals(1, assessmentMapper.getSavedResultCount());
        assertScoreEquals("53.33", assessmentMapper.getSavedAssessmentScore().getRtScore());
        assertScoreEquals("48.34", assessmentMapper.getSavedAssessmentScore().getLhScore());
        assertScoreEquals("50.00", assessmentMapper.getSavedAssessmentScore().getRpScore());
    }

    @Test
    @DisplayName("정상장 10~30% 매수는 계획 매수 규칙으로 점수를 저장한다.")
    void normalBuyAppliesPlannedBuyRule() {
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        SecurityOrderAssessmentListener listener = createListener(
                800L, 200L, 0L, List.of(), assessmentMapper
        );

        listener.onSecurityOrderFilled(createBuyEvent(BigDecimal.ZERO, 2, 200L));

        assertEquals(1, assessmentMapper.getSavedResultCount());
        assertScoreEquals("50.00", assessmentMapper.getSavedAssessmentScore().getRtScore());
        assertScoreEquals("48.34", assessmentMapper.getSavedAssessmentScore().getLhScore());
        assertScoreEquals("51.67", assessmentMapper.getSavedAssessmentScore().getRpScore());
    }

    @Test
    @DisplayName("리스너는 체결 후 DB에서 계좌 잔액을 조회한다.")
    void listenerQueriesPostTradeAccountBalance() {
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        StubTradeAccountMapper recordingMapper = new StubTradeAccountMapper(800L);
        SecurityOrderAssessmentListener listener = new SecurityOrderAssessmentListener(
                recordingMapper,
                new StubHoldingMapper(200L),
                new StubProductHoldingMapper(0L),
                createAssessmentService(List.of(), assessmentMapper)
        );

        listener.onSecurityOrderFilled(createBuyEvent(new BigDecimal("-5.50"), 2, 200L));

        assertTrue(recordingMapper.called, "체결 후 계좌 잔액을 DB에서 조회해야 한다.");
        assertEquals(1, assessmentMapper.getSavedResultCount());
    }

    @Test
    @DisplayName("성향 점수 업데이트 실패 시 예외가 전파되지 않는다.")
    void assessmentExceptionDoesNotPropagate() {
        VirtualInvestmentAssessmentService throwingService =
                new VirtualInvestmentAssessmentService(
                        null, null, null, null, null, null, null, null, null) {
                    @Override
                    public AssessmentResult updateVirtualInvestmentAssessment(
                            VirtualInvestmentBehaviorRequest req) {
                        throw new RuntimeException("성향 채점 의도적 오류");
                    }
                };
        SecurityOrderAssessmentListener listener = new SecurityOrderAssessmentListener(
                new StubTradeAccountMapper(800L),
                new StubHoldingMapper(0L),
                new StubProductHoldingMapper(0L),
                throwingService
        );

        assertDoesNotThrow(() -> listener.onSecurityOrderFilled(createBuyEvent(BigDecimal.ZERO, 2, 200L)));
    }

    // ── 헬퍼 ────────────────────────────────────────────────────────────────

    private SecurityOrderAssessmentListener createListener(
            long cash, long stockPrincipal, long deposit,
            List<VirtualInvestmentBehaviorDto> previousBehaviors,
            InMemoryAssessmentMapper assessmentMapper) {
        return new SecurityOrderAssessmentListener(
                new StubTradeAccountMapper(cash),
                new StubHoldingMapper(stockPrincipal),
                new StubProductHoldingMapper(deposit),
                createAssessmentService(previousBehaviors, assessmentMapper)
        );
    }

    private VirtualInvestmentAssessmentService createAssessmentService(
            List<VirtualInvestmentBehaviorDto> previousBehaviors,
            InMemoryAssessmentMapper assessmentMapper) {
        VirtualInvestmentBehaviorMapper behaviorMapper = new StubVirtualInvestmentBehaviorMapper(previousBehaviors);
        MarketStateCalculator marketStateCalculator = new MarketStateCalculator();
        return new VirtualInvestmentAssessmentService(
                new VirtualInvestmentBehaviorValidator(behaviorMapper),
                behaviorMapper,
                marketStateCalculator,
                new SecurityPriceRateCalculator(),
                new SecurityPositionCalculator(),
                new BehaviorContextFactory(new AssetRatioCalculator(), marketStateCalculator),
                new BehaviorRuleEngine(),
                new VirtualInvestmentScoreCalculator(),
                new AssessmentResultService(assessmentMapper, new PersonaClassifier())
        );
    }

    private SecurityOrderFilledEvent createBuyEvent(BigDecimal changeRate, int quantity, long actionAmount) {
        return new SecurityOrderFilledEvent(
                1L, 1L, 100L, "BUY", 1L, "TST",
                quantity, actionAmount,
                LocalDateTime.of(2026, 8, 12, 9, 0),
                changeRate, new BigDecimal("2.00")
        );
    }

    private SecurityOrderFilledEvent createSellEvent(BigDecimal changeRate, int quantity, long actionAmount) {
        return new SecurityOrderFilledEvent(
                1L, 1L, 100L, "SELL", 1L, "TST",
                quantity, actionAmount,
                LocalDateTime.of(2026, 8, 12, 9, 0),
                changeRate, new BigDecimal("2.00")
        );
    }

    private VirtualInvestmentBehaviorDto createPreviousBuy(long executionPrice, int quantity) {
        VirtualInvestmentBehaviorDto dto = new VirtualInvestmentBehaviorDto();
        dto.setActionType("BUY");
        dto.setAssetType("SECURITY");
        dto.setSecurityId(1L);
        dto.setStockCode("TST");
        dto.setQuantity(quantity);
        dto.setExecutionPrice(executionPrice);
        dto.setTradedAt(Timestamp.valueOf(LocalDateTime.of(2026, 8, 1, 9, 0)));
        return dto;
    }

    private void assertScoreEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "기댓값: " + expected + ", 실젯값: " + actual);
    }

    // ── 스텁 Mapper 클래스 ──────────────────────────────────────────────────

    private static class StubTradeAccountMapper implements TradeAccountMapper {
        private final long cashBalance;
        boolean called;

        StubTradeAccountMapper(long cashBalance) { this.cashBalance = cashBalance; }

        @Override public TradeAccountDto findByUserId(Long userId) {
            called = true;
            TradeAccountDto account = new TradeAccountDto();
            account.setCashBalance(cashBalance);
            return account;
        }

        @Override
        public TradeAccountDto findByAccountId(Long accountId) {
            return null;
        }

        @Override public TradeAccountDto findByAccountIdForUpdate(Long accountId) { return null; }
        @Override public int decreaseCashBalance(Long accountId, Long amount) { return 0; }
        @Override public int increaseCashBalance(Long accountId, Long amount) { return 0; }
        @Override public int increaseLocked(Long accountId, Long amount) { return 0; }
        @Override public int decreaseLocked(Long accountId, Long amount) { return 0; }
    }

    private static class StubHoldingMapper implements HoldingMapper {
        private final long stockPrincipal;

        StubHoldingMapper(long stockPrincipal) { this.stockPrincipal = stockPrincipal; }

        @Override public Long sumStockPrincipalByAccountId(Long accountId) { return stockPrincipal; }
        @Override public HoldingDto findByAccountAndSecurity(Long accountId, Long securityId) { return null; }
        @Override public List<HoldingDto> findAllByAccountId(Long accountId) { return List.of(); }
        @Override public int insert(HoldingDto holding) { return 0; }
        @Override public int updateQuantityAndAvgPrice(Long holdingSecurityId, Integer quantity, Long averagePrice) { return 0; }
        @Override public int decreaseQuantity(Long holdingSecurityId, Integer quantity) { return 0; }
        @Override public int increaseLocked(Long holdingSecurityId, Integer amount) { return 0; }
        @Override public int decreaseLocked(Long holdingSecurityId, Integer amount) { return 0; }
    }

    private static class StubProductHoldingMapper implements ProductHoldingMapper {
        private final long deposit;


        StubProductHoldingMapper(long deposit) { this.deposit = deposit;}

        @Override public Long sumActiveDepositByAccountId(Long accountId) { return deposit; }
        @Override public ProductSubscriptionInfoDto getProductSubscriptionInfo(Long userId, Long productOptionId) { return null; }
        @Override public int saveHoldingProduct(ProductHoldingCreateDto holdingProduct) { return 0; }
        @Override public int decreaseAccountCashBalance(Long accountId, BigDecimal amount) { return 0; }
        @Override public int saveProductSubscriptionTransaction(Long holdingProductId, BigDecimal amount, Integer installmentNumber) { return 0; }
        @Override public int saveAccountWithdrawalTransaction(Long accountId, BigDecimal amount) { return 0; }
        @Override public List<ProductHoldingInfoDto> getHoldingProductsByUserId(Long userId) { return List.of(); }
        @Override public List<ProductHoldingTransactionHistoryResponseDto> getProductHoldingHistory(Long userId) { return List.of(); }
        @Override public ProductHoldingInfoDto getHoldingProductForTermination(Long userId, Long holdingProductId) { return null; }
        @Override public int increaseAccountCashBalance(Long accountId, BigDecimal amount) { return 0; }
        @Override public int terminateHoldingProduct(Long holdingProductId) { return 0; }
        @Override public int saveProductTerminationTransaction(Long holdingProductId, BigDecimal amount, BigDecimal interestAmount, BigDecimal interestTaxAmount, LocalDateTime terminatedAt) { return 0; }
        @Override public int saveAccountDepositTransaction(Long accountId, BigDecimal amount) { return 0; }
        @Override public BigDecimal getAnnualInterestIncome(Long userId, LocalDateTime yearStart, LocalDateTime nextYearStart) { return null; }
        @Override public Long getLastInsertedProductTransactionId() {return null;}
        @Override
        public List<PreferentialRateConditionInfoDto> getSelectedPreferentialRateConditions(
                Long productOptionId,
                List<Long> conditionIds
        ) {
            return List.of();
        }
    }


    private static class StubVirtualInvestmentBehaviorMapper implements VirtualInvestmentBehaviorMapper {
        private final List<VirtualInvestmentBehaviorDto> previousBehaviors;

        StubVirtualInvestmentBehaviorMapper(List<VirtualInvestmentBehaviorDto> previousBehaviors) {
            this.previousBehaviors = previousBehaviors;
        }

        @Override public boolean existsAccountByUserId(Long accountId, Long userId) { return true; }
        @Override public boolean existsSecurityByIdAndStockCode(Long securityId, String stockCode) { return true; }
        @Override public boolean existsProductOption(Long productOptionId) { return true; }
        @Override public List<VirtualInvestmentBehaviorDto> getPreviousVirtualInvestmentBehaviors(
                Long accountId, Timestamp tradedAt) {
            return previousBehaviors;
        }
        @Override
        public VirtualInvestmentBehaviorRequest getProductBehaviorRequest(Long userId, Long productTransactionId) {return null;}
    }

    // ── InMemory AssessmentMapper ────────────────────────────────────────────

    private static class InMemoryAssessmentMapper implements AssessmentMapper {
        private AssessmentScore savedAssessmentScore;
        private int savedResultCount;

        @Override public AssessmentScore getLatestAssessmentScore(Long userId) { return savedAssessmentScore; }
        @Override public AssessmentResultDetails getLatestAssessmentResultDetails(Long userId) { return null; }
        @Override public AssessmentResultResponseDto getLatestAssessmentResult(Long userId) { return null; }
        @Override public Long getPersonaIdByAxisCode(String axisCode) { return 1L; }
        @Override public int saveAssessmentResult(Long userId, Long personaId, AssessmentScore score) {
            savedAssessmentScore = score;
            savedResultCount++;
            return 1;
        }

        private AssessmentScore getSavedAssessmentScore() { return savedAssessmentScore; }
        private int getSavedResultCount() { return savedResultCount; }
    }
}
