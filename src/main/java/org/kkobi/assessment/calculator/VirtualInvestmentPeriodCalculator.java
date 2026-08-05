package org.kkobi.assessment.calculator;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.dto.AccountDailySnapshotDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class VirtualInvestmentPeriodCalculator {

    private static final BigDecimal VERY_LOW_CASH_RATIO = BigDecimal.valueOf(5);
    private static final BigDecimal MEDIUM_CASH_RATIO = BigDecimal.valueOf(25);
    private static final BigDecimal HIGH_CASH_RATIO = BigDecimal.valueOf(50);
    private static final int RATIO_SCALE = 2;
    private static final int TRADE_COUNT_SCALE = 4;

    private final AssetRatioCalculator assetRatioCalculator;

    public BigDecimal calculateAverageStockRatio(List<AccountDailySnapshotDto> snapshots) {
        return calculateAverageRatio(
                snapshots,
                snapshot -> assetRatioCalculator.calculateStockRatio(
                        snapshot.getCurrentCash(),
                        snapshot.getCurrentStockPrincipal(),
                        snapshot.getCurrentDeposit()
                )
        );
    }

    public BigDecimal calculateAverageCashRatio(List<AccountDailySnapshotDto> snapshots) {
        return calculateAverageRatio(snapshots, AccountDailySnapshotDto::getCashRatio);
    }

    public BigDecimal calculateMaintainedCashRatio(List<AccountDailySnapshotDto> snapshots) {
        if (snapshots.isEmpty()) {
            return null;
        }

        BigDecimal averageCashRatio = calculateAverageCashRatio(snapshots);
        if (snapshots.stream().allMatch(this::hasVeryLowCashRatio)
                || snapshots.stream().allMatch(this::hasMediumCashRatio)
                || snapshots.stream().allMatch(this::hasHighCashRatio)) {
            return averageCashRatio;
        }
        return null;
    }

    public BigDecimal calculateAverageDailyTradeCount(
            int completedTradeCount,
            LocalDate virtualInvestmentStartedDate,
            LocalDate assessmentDate) {
        long elapsedDays = Math.max(
                1L,
                ChronoUnit.DAYS.between(virtualInvestmentStartedDate, assessmentDate)
        );
        return BigDecimal.valueOf(completedTradeCount)
                .divide(BigDecimal.valueOf(elapsedDays), TRADE_COUNT_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateAverageRatio(
            List<AccountDailySnapshotDto> snapshots,
            Function<AccountDailySnapshotDto, BigDecimal> ratioProvider) {
        if (snapshots.isEmpty()) {
            return null;
        }

        List<BigDecimal> ratios = snapshots.stream()
                .map(ratioProvider)
                .toList();
        if (ratios.stream().anyMatch(ratio -> ratio == null)) {
            return null;
        }

        BigDecimal totalRatio = ratios.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalRatio.divide(
                BigDecimal.valueOf(snapshots.size()),
                RATIO_SCALE,
                RoundingMode.HALF_UP
        );
    }

    private boolean hasVeryLowCashRatio(AccountDailySnapshotDto snapshot) {
        return snapshot.getCashRatio() != null
                && snapshot.getCashRatio().compareTo(VERY_LOW_CASH_RATIO) < 0;
    }

    private boolean hasMediumCashRatio(AccountDailySnapshotDto snapshot) {
        return snapshot.getCashRatio() != null
                && snapshot.getCashRatio().compareTo(MEDIUM_CASH_RATIO) >= 0
                && snapshot.getCashRatio().compareTo(HIGH_CASH_RATIO) < 0;
    }

    private boolean hasHighCashRatio(AccountDailySnapshotDto snapshot) {
        return snapshot.getCashRatio() != null
                && snapshot.getCashRatio().compareTo(HIGH_CASH_RATIO) >= 0;
    }
}
