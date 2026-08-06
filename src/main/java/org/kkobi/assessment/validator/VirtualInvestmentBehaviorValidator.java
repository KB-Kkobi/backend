package org.kkobi.assessment.validator;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.dto.VirtualInvestmentBehaviorRequest;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;
import org.kkobi.assessment.mapper.VirtualInvestmentBehaviorMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VirtualInvestmentBehaviorValidator {

    private final VirtualInvestmentBehaviorMapper virtualInvestmentBehaviorMapper;

    public void validateVirtualInvestmentBehavior(VirtualInvestmentBehaviorRequest request) {
        validateRequiredFields(request);
        validateAssetAmounts(request);

        BehaviorActionType actionType = BehaviorActionType.getBehaviorActionType(request.getActionType());
        BehaviorAssetType assetType = BehaviorAssetType.getBehaviorAssetType(request.getAssetType());
        validateActionAssetType(actionType, assetType);
        validateAccount(request);

        if (assetType == BehaviorAssetType.SECURITY) {
            validateSecurity(request);
        } else if (assetType == BehaviorAssetType.PRODUCT) {
            validateProduct(request);
        }
    }

    private void validateRequiredFields(VirtualInvestmentBehaviorRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("가상투자 행동 요청은 필수입니다.");
        }
        if (request.getUserId() == null
                || request.getAccountId() == null
                || request.getActionType() == null
                || request.getAssetType() == null
                || request.getActionAmount() == null
                || request.getCurrentCash() == null
                || request.getCurrentStockPrincipal() == null
                || request.getCurrentDeposit() == null
                || request.getCurrentPriceChangeRate() == null
                || request.getDailyPriceRangeRate() == null
                || request.getTradedAt() == null) {
            throw new IllegalArgumentException("가상투자 행동 요청의 필수값이 누락되었습니다.");
        }
    }

    private void validateAssetAmounts(VirtualInvestmentBehaviorRequest request) {
        if (request.getActionAmount() < 0
                || request.getCurrentCash() < 0
                || request.getCurrentStockPrincipal() < 0
                || request.getCurrentDeposit() < 0) {
            throw new IllegalArgumentException("거래 금액과 행동 후 자산 금액은 0 이상이어야 합니다.");
        }
    }

    private void validateActionAssetType(
            BehaviorActionType actionType,
            BehaviorAssetType assetType) {
        boolean securityAction = actionType == BehaviorActionType.BUY
                || actionType == BehaviorActionType.SELL;
        if (securityAction && assetType != BehaviorAssetType.SECURITY) {
            throw new IllegalArgumentException("매수와 매도 행동의 assetType은 SECURITY여야 합니다.");
        }

        boolean productAction = actionType == BehaviorActionType.JOIN_PRODUCT
                || actionType == BehaviorActionType.CANCEL_PRODUCT
                || actionType == BehaviorActionType.MATURITY;
        if (productAction && assetType != BehaviorAssetType.PRODUCT) {
            throw new IllegalArgumentException("예적금 행동의 assetType은 PRODUCTS여야 합니다.");
        }

        if (!securityAction && !productAction) {
            throw new IllegalArgumentException("가상투자에서 지원하지 않는 행동입니다: " + actionType);
        }
    }

    private void validateAccount(VirtualInvestmentBehaviorRequest request) {
        if (!virtualInvestmentBehaviorMapper.existsAccountByUserId(
                request.getAccountId(),
                request.getUserId())) {
            throw new IllegalArgumentException("사용자에게 속한 가상계좌를 찾을 수 없습니다.");
        }
    }

    private void validateSecurity(VirtualInvestmentBehaviorRequest request) {
        if (request.getSecurityId() == null
                || request.getStockCode() == null
                || request.getStockCode().isBlank()
                || request.getQuantity() == null
                || request.getQuantity() <= 0
                || request.getActionAmount() <= 0) {
            throw new IllegalArgumentException("증권 행동에는 증권 식별값, 수량, 거래 금액이 필요합니다.");
        }

        if (!virtualInvestmentBehaviorMapper.existsSecurityByIdAndStockCode(
                request.getSecurityId(),
                request.getStockCode())) {
            throw new IllegalArgumentException("증권 ID와 종목 코드가 일치하지 않습니다.");
        }
    }

    private void validateProduct(VirtualInvestmentBehaviorRequest request) {
        if (request.getProductOptionId() == null) {
            throw new IllegalArgumentException("예적금 행동에는 productOptionId가 필요합니다.");
        }
        if (!virtualInvestmentBehaviorMapper.existsProductOption(request.getProductOptionId())) {
            throw new IllegalArgumentException("금융상품 옵션을 찾을 수 없습니다.");
        }
    }
}
