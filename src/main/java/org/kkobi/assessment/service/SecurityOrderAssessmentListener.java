package org.kkobi.assessment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kkobi.assessment.dto.VirtualInvestmentBehaviorRequest;
import org.kkobi.product.mapper.ProductHoldingMapper;
import org.kkobi.trade.dto.TradeAccountDto;
import org.kkobi.trade.event.SecurityOrderFilledEvent;
import org.kkobi.trade.mapper.HoldingMapper;
import org.kkobi.trade.mapper.TradeAccountMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityOrderAssessmentListener {

    private final TradeAccountMapper accountMapper;
    private final HoldingMapper holdingMapper;
    private final ProductHoldingMapper productHoldingMapper;
    private final VirtualInvestmentAssessmentService assessmentService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSecurityOrderFilled(SecurityOrderFilledEvent event) {
        try {
            VirtualInvestmentBehaviorRequest request = buildRequest(event);
            assessmentService.updateVirtualInvestmentAssessment(request);
        } catch (Exception e) {
            log.warn("성향 점수 업데이트 실패 securityOrderId={} userId={} actionType={} error={}",
                    event.securityOrderId(), event.userId(), event.actionType(), e.getMessage());
        }
    }

    private VirtualInvestmentBehaviorRequest buildRequest(SecurityOrderFilledEvent event) {
        TradeAccountDto account = accountMapper.findByUserId(event.userId());
        Long currentCash = account.getCashBalance();
        Long currentStockPrincipal = holdingMapper.sumStockPrincipalByAccountId(event.accountId());
        Long currentDeposit = productHoldingMapper.sumActiveDepositByAccountId(event.accountId());

        VirtualInvestmentBehaviorRequest request = new VirtualInvestmentBehaviorRequest();
        request.setUserId(event.userId());
        request.setAccountId(event.accountId());
        request.setReferenceType("SECURITY_ORDER");
        request.setReferenceId(event.securityOrderId());
        request.setActionType(event.actionType());
        request.setAssetType("SECURITY");
        request.setSecurityId(event.securityId());
        request.setStockCode(event.stockCode());
        request.setQuantity(event.quantity());
        request.setActionAmount(event.actionAmount());
        request.setCurrentCash(currentCash);
        request.setCurrentStockPrincipal(currentStockPrincipal);
        request.setCurrentDeposit(currentDeposit);
        request.setCurrentPriceChangeRate(event.currentPriceChangeRate());
        request.setDailyPriceRangeRate(event.dailyPriceRangeRate());
        request.setTradedAt(event.tradedAt());
        return request;
    }
}
