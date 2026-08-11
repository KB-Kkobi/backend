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
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.dto.AssessmentResultResponseDto;
import org.kkobi.assessment.dto.VirtualInvestmentBehaviorDto;
import org.kkobi.assessment.dto.VirtualInvestmentBehaviorRequest;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.mapper.AssessmentMapper;
import org.kkobi.assessment.mapper.VirtualInvestmentBehaviorMapper;
import org.kkobi.assessment.validator.VirtualInvestmentBehaviorValidator;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualInvestmentAssessmentServiceTest {

    @Test
    @DisplayName("매도 손익률이 없어도 거래 이력으로 손절 규칙을 계산한다.")
    void updateVirtualInvestmentAssessmentCalculatesSellReturnRate() {
        VirtualInvestmentBehaviorMapper behaviorMapper = createBehaviorMapper();
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        VirtualInvestmentAssessmentService assessmentService = createAssessmentService(
                behaviorMapper,
                assessmentMapper
        );

        AssessmentResult result = assessmentService.updateVirtualInvestmentAssessment(
                createSellRequest()
        );

        assertTrue(result.getAppliedRules().stream()
                .anyMatch(rule -> rule.getRuleCode() == BehaviorRuleCode.LOSS_CUT_SELL));
        assertScoreEquals("50.00", result.getAssessmentScore().getRtScore());
        assertScoreEquals("53.33", result.getAssessmentScore().getLhScore());
        assertScoreEquals("51.67", result.getAssessmentScore().getRpScore());
        assertEquals(1, assessmentMapper.getSavedResultCount());
        assertEquals(result.getAssessmentScore(), assessmentMapper.getSavedAssessmentScore());
    }

    @Test
    @DisplayName("급락장 매수 요청을 EMA 점수로 계산해 결과를 저장한다.")
    void updateVirtualInvestmentAssessmentSavesCrashBuyEmaResult() {
        VirtualInvestmentBehaviorMapper behaviorMapper = createBehaviorMapper(List.of());
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        VirtualInvestmentAssessmentService assessmentService = createAssessmentService(
                behaviorMapper,
                assessmentMapper
        );

        AssessmentResult result = assessmentService.updateVirtualInvestmentAssessment(
                createCrashBuyRequest()
        );

        assertTrue(result.getAppliedRules().stream()
                .anyMatch(rule -> rule.getRuleCode() == BehaviorRuleCode.CRASH_BUY));
        assertScoreEquals("53.33", result.getAssessmentScore().getRtScore());
        assertScoreEquals("48.34", result.getAssessmentScore().getLhScore());
        assertScoreEquals("51.67", result.getAssessmentScore().getRpScore());
        assertEquals(1, assessmentMapper.getSavedResultCount());
        assertEquals(result.getAssessmentScore(), assessmentMapper.getSavedAssessmentScore());
    }

    private VirtualInvestmentAssessmentService createAssessmentService(
            VirtualInvestmentBehaviorMapper behaviorMapper,
            AssessmentMapper assessmentMapper) {
        MarketStateCalculator marketStateCalculator = new MarketStateCalculator();
        SecurityPriceRateCalculator securityPriceRateCalculator = new SecurityPriceRateCalculator();
        return new VirtualInvestmentAssessmentService(
                new VirtualInvestmentBehaviorValidator(behaviorMapper),
                behaviorMapper,
                marketStateCalculator,
                securityPriceRateCalculator,
                new SecurityPositionCalculator(),
                new BehaviorContextFactory(
                        new AssetRatioCalculator(),
                        marketStateCalculator
                ),
                new BehaviorRuleEngine(),
                new VirtualInvestmentScoreCalculator(),
                new AssessmentResultService(
                        assessmentMapper,
                        new PersonaClassifier()
                )
        );
    }

    private VirtualInvestmentBehaviorRequest createSellRequest() {
        VirtualInvestmentBehaviorRequest request = new VirtualInvestmentBehaviorRequest();
        request.setUserId(1L);
        request.setAccountId(1L);
        request.setReferenceType("SECURITY_ORDER");
        request.setReferenceId(1L);
        request.setActionType("SELL");
        request.setAssetType("SECURITY");
        request.setSecurityId(1L);
        request.setStockCode("TEST");
        request.setQuantity(4);
        request.setActionAmount(320L);
        request.setCurrentCash(320L);
        request.setCurrentStockPrincipal(600L);
        request.setCurrentDeposit(0L);
        request.setCurrentPriceChangeRate(BigDecimal.ZERO);
        request.setDailyPriceRangeRate(BigDecimal.ZERO);
        request.setTradedAt(LocalDateTime.of(2026, 8, 2, 9, 0));
        return request;
    }

    private VirtualInvestmentBehaviorRequest createCrashBuyRequest() {
        VirtualInvestmentBehaviorRequest request = new VirtualInvestmentBehaviorRequest();
        request.setUserId(1L);
        request.setAccountId(1L);
        request.setReferenceType("SECURITY_ORDER");
        request.setReferenceId(2L);
        request.setActionType("BUY");
        request.setAssetType("SECURITY");
        request.setSecurityId(1L);
        request.setStockCode("TEST");
        request.setQuantity(2);
        request.setActionAmount(200L);
        request.setCurrentCash(800L);
        request.setCurrentStockPrincipal(200L);
        request.setCurrentDeposit(0L);
        request.setCurrentPriceChangeRate(new BigDecimal("-5.00"));
        request.setDailyPriceRangeRate(BigDecimal.ZERO);
        request.setTradedAt(LocalDateTime.of(2026, 8, 2, 9, 0));
        return request;
    }

    private VirtualInvestmentBehaviorMapper createBehaviorMapper() {
        VirtualInvestmentBehaviorDto previousBuy = new VirtualInvestmentBehaviorDto();
        previousBuy.setActionType("BUY");
        previousBuy.setAssetType("SECURITY");
        previousBuy.setSecurityId(1L);
        previousBuy.setStockCode("TEST");
        previousBuy.setQuantity(10);
        previousBuy.setExecutionPrice(100L);
        previousBuy.setTradedAt(Timestamp.valueOf(
                LocalDateTime.of(2026, 8, 1, 9, 0)
        ));
        return createBehaviorMapper(List.of(previousBuy));
    }

    private VirtualInvestmentBehaviorMapper createBehaviorMapper(
            List<VirtualInvestmentBehaviorDto> previousBehaviors) {
        return new VirtualInvestmentBehaviorMapper() {
            @Override
            public boolean existsAccountByUserId(Long accountId, Long userId) {
                return true;
            }

            @Override
            public boolean existsSecurityByIdAndStockCode(Long securityId, String stockCode) {
                return true;
            }

            @Override
            public boolean existsProductOption(Long productOptionId) {
                return true;
            }

            @Override
            public List<VirtualInvestmentBehaviorDto> getPreviousVirtualInvestmentBehaviors(
                    Long accountId,
                    Timestamp tradedAt) {
                return previousBehaviors;
            }
        };
    }

    private void assertScoreEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
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
        public AssessmentResultResponseDto getLatestAssessmentResult(Long userId) {
            return null;
        }

        @Override
        public Long getPersonaIdByAxisCode(String axisCode) {
            return 1L;
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
