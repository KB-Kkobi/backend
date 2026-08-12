package org.kkobi.assessment.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.calculator.BehaviorContextFactory;
import org.kkobi.assessment.calculator.BehaviorRuleEngine;
import org.kkobi.assessment.calculator.MarketStateCalculator;
import org.kkobi.assessment.calculator.SecurityPriceRateCalculator;
import org.kkobi.assessment.calculator.SecurityPositionCalculator;
import org.kkobi.assessment.calculator.VirtualInvestmentScoreCalculator;
import org.kkobi.assessment.domain.AssessmentResult;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.domain.BehaviorAnalysisResult;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.domain.BehaviorEvent;
import org.kkobi.assessment.dto.VirtualInvestmentBehaviorDto;
import org.kkobi.assessment.dto.VirtualInvestmentBehaviorRequest;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;
import org.kkobi.assessment.enums.MarketState;
import org.kkobi.assessment.mapper.VirtualInvestmentBehaviorMapper;
import org.kkobi.assessment.validator.VirtualInvestmentBehaviorValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VirtualInvestmentAssessmentService {

    private final VirtualInvestmentBehaviorValidator virtualInvestmentBehaviorValidator;
    private final VirtualInvestmentBehaviorMapper virtualInvestmentBehaviorMapper;
    private final MarketStateCalculator marketStateCalculator;
    private final SecurityPriceRateCalculator securityPriceRateCalculator;
    private final SecurityPositionCalculator securityPositionCalculator;
    private final BehaviorContextFactory behaviorContextFactory;
    private final BehaviorRuleEngine behaviorRuleEngine;
    private final VirtualInvestmentScoreCalculator virtualInvestmentScoreCalculator;
    private final AssessmentResultService assessmentResultService;

    @Transactional
    public AssessmentResult updateVirtualInvestmentAssessment(
            VirtualInvestmentBehaviorRequest request) {
        virtualInvestmentBehaviorValidator.validateVirtualInvestmentBehavior(request);
        BehaviorEvent currentEvent = createBehaviorEvent(request);
        List<BehaviorEvent> previousEvents = virtualInvestmentBehaviorMapper
                .getPreviousVirtualInvestmentBehaviors(
                        request.getAccountId(),
                        Timestamp.valueOf(request.getTradedAt())
                )
                .stream()
                .map(this::createBehaviorEvent)
                .toList();
        updateSecurityPosition(currentEvent, previousEvents);

        BehaviorContext behaviorContext = behaviorContextFactory.createBehaviorContext(
                currentEvent,
                previousEvents
        );
        BehaviorAnalysisResult analysisResult = behaviorRuleEngine
                .calculateVirtualInvestmentBehaviorAnalysis(behaviorContext);
        AssessmentScore currentScore = assessmentResultService.getLatestAssessmentScore(request.getUserId());
        if (!analysisResult.existsAppliedRule()) {
            return assessmentResultService.createAssessmentResult(currentScore, List.of());
        }

        AssessmentScore updatedScore = virtualInvestmentScoreCalculator.calculateVirtualInvestmentScore(
                currentScore,
                analysisResult.getTotalScoreDelta()
        );
        return assessmentResultService.saveAssessmentResult(
                request.getUserId(),
                updatedScore,
                analysisResult.getAppliedRules()
        );
    }

    private BehaviorEvent createBehaviorEvent(VirtualInvestmentBehaviorRequest request) {
        BehaviorEvent event = new BehaviorEvent();
        event.setUserId(request.getUserId());
        event.setAccountId(request.getAccountId());
        event.setActionType(BehaviorActionType.getBehaviorActionType(request.getActionType()));
        event.setAssetType(BehaviorAssetType.getBehaviorAssetType(request.getAssetType()));
        event.setSecurityId(request.getSecurityId());
        event.setStockCode(request.getStockCode());
        event.setProductOptionId(request.getProductOptionId());
        event.setQuantity(request.getQuantity());
        event.setActionAmount(request.getActionAmount());
        event.setCurrentCash(request.getCurrentCash());
        event.setCurrentStockPrincipal(request.getCurrentStockPrincipal());
        event.setCurrentDeposit(request.getCurrentDeposit());
        event.setCurrentPriceChangeRate(request.getCurrentPriceChangeRate());
        event.setDailyPriceRangeRate(request.getDailyPriceRangeRate());
        event.setRealizedReturnRate(request.getRealizedReturnRate());
        event.setTradedAt(request.getTradedAt());
        return event;
    }

    private BehaviorEvent createBehaviorEvent(VirtualInvestmentBehaviorDto behavior) {
        BehaviorEvent event = new BehaviorEvent();
        event.setActionType(BehaviorActionType.getBehaviorActionType(behavior.getActionType()));
        event.setAssetType(BehaviorAssetType.getBehaviorAssetType(behavior.getAssetType()));
        event.setSecurityId(behavior.getSecurityId());
        event.setStockCode(behavior.getStockCode());
        event.setProductOptionId(behavior.getProductOptionId());
        event.setQuantity(behavior.getQuantity());
        event.setActionAmount(behavior.getActionAmount());
        event.setExecutionPrice(behavior.getExecutionPrice());
        event.setCurrentPriceChangeRate(securityPriceRateCalculator.calculatePriceChangeRate(
                behavior.getPreviousClosePrice(),
                behavior.getCurrentClosePrice()
        ));
        event.setDailyPriceRangeRate(securityPriceRateCalculator.calculateDailyPriceRangeRate(
                behavior.getOpenPrice(),
                behavior.getHighPrice(),
                behavior.getLowPrice()
        ));
        if (behavior.getTradedAt() != null) {
            event.setTradedAt(behavior.getTradedAt().toLocalDateTime());
        }
        event.setMarketState(calculatePreviousMarketState(event));
        return event;
    }

    private MarketState calculatePreviousMarketState(BehaviorEvent event) {
        if (event.getAssetType() != BehaviorAssetType.SECURITY
                || event.getCurrentPriceChangeRate() == null
                || event.getDailyPriceRangeRate() == null) {
            return MarketState.NORMAL;
        }
        return marketStateCalculator.calculateMarketState(
                event.getCurrentPriceChangeRate(),
                event.getDailyPriceRangeRate()
        );
    }

    private void updateSecurityPosition(
            BehaviorEvent currentEvent,
            List<BehaviorEvent> previousEvents) {
        if (currentEvent.getAssetType() != BehaviorAssetType.SECURITY) {
            return;
        }

        BigDecimal currentReturnRate = securityPositionCalculator.calculatePositionReturnRate(
                currentEvent,
                previousEvents
        );
        if (currentEvent.getActionType() == BehaviorActionType.BUY) {
            currentEvent.setPositionReturnRate(currentReturnRate);
        } else if (currentEvent.getActionType() == BehaviorActionType.SELL
                && currentReturnRate != null) {
            currentEvent.setRealizedReturnRate(currentReturnRate);
        }
        currentEvent.setCurrentSecurityQuantity(
                securityPositionCalculator.calculateCurrentSecurityQuantity(currentEvent, previousEvents)
        );
    }
}
