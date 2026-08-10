package org.kkobi.assessment.calculator;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class AssetRatioCalculator {

    private static final BigDecimal PERCENTAGE = BigDecimal.valueOf(100);
    private static final int RATIO_SCALE = 2;

    public BigDecimal calculateCashRatio(Long currentCash, Long currentStockPrincipal, Long currentDeposit) {
        return calculateAssetRatio(currentCash, currentCash, currentStockPrincipal, currentDeposit);
    }

    public BigDecimal calculateStockRatio(Long currentCash, Long currentStockPrincipal, Long currentDeposit) {
        return calculateAssetRatio(currentStockPrincipal, currentCash, currentStockPrincipal, currentDeposit);
    }

    public BigDecimal calculateDepositRatio(Long currentCash, Long currentStockPrincipal, Long currentDeposit) {
        return calculateAssetRatio(currentDeposit, currentCash, currentStockPrincipal, currentDeposit);
    }

    private BigDecimal calculateAssetRatio(
            Long assetPrincipal,
            Long currentCash,
            Long currentStockPrincipal,
            Long currentDeposit) {
        BigDecimal totalPrincipal = BigDecimal.valueOf(currentCash)
                .add(BigDecimal.valueOf(currentStockPrincipal))
                .add(BigDecimal.valueOf(currentDeposit));

        if (totalPrincipal.signum() == 0) {
            return BigDecimal.ZERO.setScale(RATIO_SCALE, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(assetPrincipal)
                .multiply(PERCENTAGE)
                .divide(totalPrincipal, RATIO_SCALE, RoundingMode.HALF_UP);
    }
}
