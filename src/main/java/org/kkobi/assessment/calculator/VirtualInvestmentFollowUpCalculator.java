package org.kkobi.assessment.calculator;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.dto.AccountDailySnapshotDto;
import org.kkobi.assessment.dto.VirtualInvestmentBehaviorDto;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.MarketState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class VirtualInvestmentFollowUpCalculator {

    private static final long FOLLOW_UP_HOURS = 48;
    private static final int CASH_RETENTION_DAYS = 2;
    private static final int OPPORTUNITY_COMPLETION_DAYS = 10;
    private static final BigDecimal TWENTY = BigDecimal.valueOf(20);
    private static final BigDecimal TWENTY_FIVE = BigDecimal.valueOf(25);
    private static final BigDecimal FIFTY = BigDecimal.valueOf(50);
    private static final BigDecimal EIGHTY = BigDecimal.valueOf(80);
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final MarketStateCalculator marketStateCalculator;
    private final SecurityPositionCalculator securityPositionCalculator;

    public BehaviorContext calculateFollowUpContext(
            List<VirtualInvestmentBehaviorDto> behaviors,
            List<AccountDailySnapshotDto> snapshots,
            LocalDate assessmentDate) {
        List<VirtualInvestmentBehaviorDto> orderedBehaviors = behaviors.stream()
                .filter(behavior -> behavior.getTradedAt() != null)
                .sorted(Comparator.comparing(VirtualInvestmentBehaviorDto::getTradedAt))
                .toList();
        Map<LocalDate, AccountDailySnapshotDto> snapshotByDate = new HashMap<>();
        snapshots.forEach(snapshot -> snapshotByDate.put(snapshot.getSnapshotDate(), snapshot));

        BehaviorContext context = new BehaviorContext();
        context.setDepositCancelledBeforeSecurityBuy(
                hasDepositCancelThenBuy(orderedBehaviors, assessmentDate)
        );
        context.setDepositCancelCashRetention(
                hasDepositCancelCashRetention(orderedBehaviors, snapshotByDate, assessmentDate)
        );
        context.setNormalPartialSellCashRetention(
                hasNormalPartialSellCashRetention(orderedBehaviors, snapshotByDate, assessmentDate)
        );
        context.setCompletedLiquidityOpportunity(
                hasCompletedLiquidityOpportunity(orderedBehaviors, snapshotByDate, assessmentDate)
        );
        return context;
    }

    private boolean hasDepositCancelThenBuy(
            List<VirtualInvestmentBehaviorDto> behaviors,
            LocalDate assessmentDate) {
        return behaviors.stream()
                .filter(behavior -> actionType(behavior) == BehaviorActionType.BUY)
                .filter(behavior -> assessmentDate.equals(tradeDate(behavior)))
                .anyMatch(buy -> behaviors.stream()
                        .filter(cancel -> actionType(cancel) == BehaviorActionType.CANCEL_PRODUCT)
                        .anyMatch(cancel -> isWithinHours(cancel, buy, FOLLOW_UP_HOURS)));
    }

    private boolean hasDepositCancelCashRetention(
            List<VirtualInvestmentBehaviorDto> behaviors,
            Map<LocalDate, AccountDailySnapshotDto> snapshotByDate,
            LocalDate assessmentDate) {
        LocalDate cancelDate = assessmentDate.minusDays(CASH_RETENTION_DAYS);
        return behaviors.stream()
                .filter(behavior -> actionType(behavior) == BehaviorActionType.CANCEL_PRODUCT)
                .filter(behavior -> cancelDate.equals(tradeDate(behavior)))
                .anyMatch(cancel -> {
                    AccountDailySnapshotDto baseSnapshot = snapshotByDate.get(cancelDate);
                    if (baseSnapshot == null || baseSnapshot.getCurrentCash() == null) {
                        return false;
                    }
                    boolean retainedCash = List.of(1, 2).stream()
                            .map(cancelDate::plusDays)
                            .map(snapshotByDate::get)
                            .allMatch(snapshot -> snapshot != null
                                    && percentage(snapshot.getCurrentCash(), baseSnapshot.getCurrentCash())
                                    .compareTo(EIGHTY) >= 0);
                    boolean boughtSecurity = behaviors.stream()
                            .filter(behavior -> actionType(behavior) == BehaviorActionType.BUY)
                            .anyMatch(buy -> isAfterAndNotLaterThan(cancel, buy, assessmentDate));
                    return retainedCash && !boughtSecurity;
                });
    }

    private boolean hasNormalPartialSellCashRetention(
            List<VirtualInvestmentBehaviorDto> behaviors,
            Map<LocalDate, AccountDailySnapshotDto> snapshotByDate,
            LocalDate assessmentDate) {
        LocalDate sellDate = assessmentDate.minusDays(CASH_RETENTION_DAYS);
        Map<Long, Integer> quantities = new HashMap<>();
        for (VirtualInvestmentBehaviorDto behavior : behaviors) {
            if (behavior.getSecurityId() == null || behavior.getQuantity() == null) {
                continue;
            }
            int quantityBefore = quantities.getOrDefault(behavior.getSecurityId(), 0);
            if (actionType(behavior) == BehaviorActionType.BUY) {
                quantities.put(behavior.getSecurityId(), quantityBefore + behavior.getQuantity());
                continue;
            }
            if (actionType(behavior) != BehaviorActionType.SELL) {
                continue;
            }
            boolean targetSell = sellDate.equals(tradeDate(behavior))
                    && calculateSellRatio(behavior.getQuantity(), quantityBefore)
                    .compareTo(TWENTY) >= 0
                    && calculateSellRatio(behavior.getQuantity(), quantityBefore)
                    .compareTo(FIFTY) <= 0
                    && calculateMarketState(behavior) == MarketState.NORMAL;
            quantities.put(
                    behavior.getSecurityId(),
                    Math.max(0, quantityBefore - behavior.getQuantity())
            );
            if (targetSell && maintainsCashRatio(snapshotByDate, sellDate.plusDays(1), 2)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCompletedLiquidityOpportunity(
            List<VirtualInvestmentBehaviorDto> behaviors,
            Map<LocalDate, AccountDailySnapshotDto> snapshotByDate,
            LocalDate assessmentDate) {
        Map<Long, SecurityPositionCalculator.SecurityPosition> positions = new HashMap<>();
        Map<Long, LocalDate> latestLiquidityBuyDates = new HashMap<>();
        for (VirtualInvestmentBehaviorDto behavior : behaviors) {
            if (behavior.getSecurityId() == null
                    || behavior.getQuantity() == null
                    || behavior.getQuantity() <= 0
                    || behavior.getExecutionPrice() == null) {
                continue;
            }
            SecurityPositionCalculator.SecurityPosition position = positions.computeIfAbsent(
                    behavior.getSecurityId(),
                    ignored -> securityPositionCalculator.createSecurityPosition()
            );
            if (actionType(behavior) == BehaviorActionType.BUY) {
                position.buy(behavior.getQuantity(), behavior.getExecutionPrice());
                if (isCashRatioBetween(
                        snapshotByDate.get(tradeDate(behavior)),
                        TWENTY_FIVE,
                        FIFTY
                )) {
                    latestLiquidityBuyDates.put(
                            behavior.getSecurityId(),
                            tradeDate(behavior)
                    );
                }
                continue;
            }
            if (actionType(behavior) != BehaviorActionType.SELL) {
                continue;
            }
            boolean completed = assessmentDate.equals(tradeDate(behavior))
                    && position.isProfitable(behavior.getExecutionPrice())
                    && hasRecentLiquidityBuy(
                            latestLiquidityBuyDates.get(behavior.getSecurityId()),
                            assessmentDate,
                            OPPORTUNITY_COMPLETION_DAYS
                    );
            position.sell(behavior.getQuantity());
            if (position.getQuantity() == 0) {
                latestLiquidityBuyDates.remove(behavior.getSecurityId());
            }
            if (completed) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRecentLiquidityBuy(
            LocalDate latestLiquidityBuyDate,
            LocalDate sellDate,
            int maximumDays) {
        return latestLiquidityBuyDate != null
                && !latestLiquidityBuyDate.isAfter(sellDate)
                && !latestLiquidityBuyDate.isBefore(sellDate.minusDays(maximumDays));
    }

    private boolean maintainsCashRatio(
            Map<LocalDate, AccountDailySnapshotDto> snapshotByDate,
            LocalDate startDate,
            int days) {
        for (int offset = 0; offset < days; offset++) {
            if (!isCashRatioBetween(
                    snapshotByDate.get(startDate.plusDays(offset)),
                    TWENTY_FIVE,
                    FIFTY
            )) {
                return false;
            }
        }
        return true;
    }

    private boolean isCashRatioBetween(
            AccountDailySnapshotDto snapshot,
            BigDecimal minimum,
            BigDecimal maximum) {
        return snapshot != null
                && snapshot.getCashRatio() != null
                && snapshot.getCashRatio().compareTo(minimum) >= 0
                && snapshot.getCashRatio().compareTo(maximum) < 0;
    }

    private MarketState calculateMarketState(VirtualInvestmentBehaviorDto behavior) {
        BigDecimal changeRate = calculateRate(
                behavior.getCurrentClosePrice(),
                behavior.getPreviousClosePrice()
        );
        BigDecimal dailyRangeRate = behavior.getOpenPrice() == null
                ? null
                : calculatePercentage(
                        subtract(behavior.getHighPrice(), behavior.getLowPrice()),
                        behavior.getOpenPrice()
                );
        return marketStateCalculator.calculateMarketState(changeRate, dailyRangeRate);
    }

    private BigDecimal subtract(BigDecimal left, BigDecimal right) {
        return left == null || right == null ? null : left.subtract(right);
    }

    private BigDecimal calculateRate(BigDecimal amount, BigDecimal base) {
        if (amount == null || base == null || base.signum() == 0) {
            return null;
        }
        return amount.subtract(base)
                .multiply(ONE_HUNDRED)
                .divide(base, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculatePercentage(BigDecimal amount, BigDecimal base) {
        if (amount == null || base == null || base.signum() == 0) {
            return null;
        }
        return amount.multiply(ONE_HUNDRED)
                .divide(base, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateSellRatio(int sellQuantity, int quantityBefore) {
        return percentage(sellQuantity, quantityBefore);
    }

    private BigDecimal percentage(long amount, long total) {
        if (amount < 0 || total <= 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(amount)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP);
    }

    private boolean isWithinHours(
            VirtualInvestmentBehaviorDto previous,
            VirtualInvestmentBehaviorDto current,
            long hours) {
        if (previous.getTradedAt().after(current.getTradedAt())) {
            return false;
        }
        return Duration.between(
                previous.getTradedAt().toLocalDateTime(),
                current.getTradedAt().toLocalDateTime()
        ).toHours() <= hours;
    }

    private boolean isAfterAndNotLaterThan(
            VirtualInvestmentBehaviorDto previous,
            VirtualInvestmentBehaviorDto current,
            LocalDate endDate) {
        return !current.getTradedAt().before(previous.getTradedAt())
                && !tradeDate(current).isAfter(endDate);
    }

    private BehaviorActionType actionType(VirtualInvestmentBehaviorDto behavior) {
        return BehaviorActionType.getBehaviorActionType(behavior.getActionType());
    }

    private LocalDate tradeDate(VirtualInvestmentBehaviorDto behavior) {
        return behavior.getTradedAt().toLocalDateTime().toLocalDate();
    }
}
