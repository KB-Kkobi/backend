package org.kkobi.assessment.calculator;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.domain.BehaviorEvent;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;
import org.kkobi.assessment.enums.MarketState;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BehaviorContextFactory {

    private static final long RELATED_ACTION_HOURS = 24;

    private final AssetRatioCalculator assetRatioCalculator;
    private final MarketStateCalculator marketStateCalculator;

    public BehaviorContext createBehaviorContext(
            BehaviorEvent currentEvent,
            List<BehaviorEvent> previousEvents) {
        List<BehaviorEvent> sortedPreviousEvents = previousEvents.stream()
                .filter(event -> event.getTradedAt() != null)
                .sorted(Comparator.comparing(BehaviorEvent::getTradedAt).reversed())
                .toList();
        MarketState marketState = calculateMarketState(currentEvent);

        currentEvent.setMarketState(marketState);

        BehaviorContext context = new BehaviorContext();
        context.setCurrentEvent(currentEvent);
        context.setPreviousEvents(sortedPreviousEvents);
        context.setInitialAllocation(currentEvent.getActionType() == BehaviorActionType.INITIAL_ALLOCATION);
        context.setFullSecuritySell(calculateFullSecuritySell(currentEvent));
        context.setSameDayTrade(calculateSameDayTrade(currentEvent, sortedPreviousEvents));
        context.setDepositCancelledBeforeSecurityBuy(
                calculateDepositCancelledBeforeSecurityBuy(currentEvent, sortedPreviousEvents)
        );
        context.setStockRotation(calculateStockRotation(currentEvent, sortedPreviousEvents));
        context.setDepositMatured(currentEvent.getActionType() == BehaviorActionType.MATURITY);
        context.setStockRatio(calculateStockRatio(currentEvent));
        context.setCashRatio(calculateCashRatio(currentEvent));
        context.setDepositRatio(calculateDepositRatio(currentEvent));
        context.setAverageHoldingDays(calculateHoldingDays(currentEvent, sortedPreviousEvents));
        context.setMarketState(marketState);
        context.setConsecutiveActionCount(
                calculateConsecutiveActionCount(currentEvent, sortedPreviousEvents, marketState)
        );
        return context;
    }

    private MarketState calculateMarketState(BehaviorEvent currentEvent) {
        if (currentEvent.getMarketState() != null) {
            return currentEvent.getMarketState();
        }
        return marketStateCalculator.calculateMarketState(
                currentEvent.getCurrentPriceChangeRate(),
                currentEvent.getDailyPriceRangeRate()
        );
    }

    private boolean calculateFullSecuritySell(BehaviorEvent currentEvent) {
        if (currentEvent.getActionType() != BehaviorActionType.SELL
                || currentEvent.getAssetType() != BehaviorAssetType.SECURITY) {
            return false;
        }

        if (currentEvent.getCurrentSecurityQuantity() != null) {
            return currentEvent.getCurrentSecurityQuantity() == 0;
        }

        return currentEvent.getCurrentStockPrincipal() != null
                && currentEvent.getCurrentStockPrincipal() == 0L;
    }

    private boolean calculateSameDayTrade(
            BehaviorEvent currentEvent,
            List<BehaviorEvent> previousEvents) {
        if (currentEvent.getActionType() != BehaviorActionType.SELL
                || currentEvent.getSecurityId() == null
                || currentEvent.getTradedAt() == null) {
            return false;
        }

        return previousEvents.stream()
                .filter(event -> event.getActionType() == BehaviorActionType.BUY)
                .filter(event -> currentEvent.getSecurityId().equals(event.getSecurityId()))
                .filter(event -> event.getTradedAt() != null)
                .anyMatch(event -> event.getTradedAt().toLocalDate()
                        .equals(currentEvent.getTradedAt().toLocalDate()));
    }

    private boolean calculateDepositCancelledBeforeSecurityBuy(
            BehaviorEvent currentEvent,
            List<BehaviorEvent> previousEvents) {
        if (currentEvent.getActionType() != BehaviorActionType.BUY
                || currentEvent.getAssetType() != BehaviorAssetType.SECURITY) {
            return false;
        }

        return previousEvents.stream()
                .filter(event -> event.getActionType() == BehaviorActionType.CANCEL_PRODUCT)
                .anyMatch(event -> isRelatedActionTime(event.getTradedAt(), currentEvent.getTradedAt()));
    }

    private boolean calculateStockRotation(
            BehaviorEvent currentEvent,
            List<BehaviorEvent> previousEvents) {
        if (currentEvent.getActionType() != BehaviorActionType.BUY
                || currentEvent.getAssetType() != BehaviorAssetType.SECURITY
                || currentEvent.getSecurityId() == null) {
            return false;
        }

        return previousEvents.stream()
                .filter(event -> event.getActionType() == BehaviorActionType.SELL)
                .filter(event -> event.getSecurityId() != null)
                .filter(event -> !currentEvent.getSecurityId().equals(event.getSecurityId()))
                .anyMatch(event -> isRelatedActionTime(event.getTradedAt(), currentEvent.getTradedAt()));
    }

    private BigDecimal calculateStockRatio(BehaviorEvent event) {
        if (!hasAssetSnapshot(event)) {
            return null;
        }
        return assetRatioCalculator.calculateStockRatio(
                event.getCurrentCash(),
                event.getCurrentStockPrincipal(),
                event.getCurrentDeposit()
        );
    }

    private BigDecimal calculateCashRatio(BehaviorEvent event) {
        if (!hasAssetSnapshot(event)) {
            return null;
        }
        return assetRatioCalculator.calculateCashRatio(
                event.getCurrentCash(),
                event.getCurrentStockPrincipal(),
                event.getCurrentDeposit()
        );
    }

    private BigDecimal calculateDepositRatio(BehaviorEvent event) {
        if (!hasAssetSnapshot(event)) {
            return null;
        }
        return assetRatioCalculator.calculateDepositRatio(
                event.getCurrentCash(),
                event.getCurrentStockPrincipal(),
                event.getCurrentDeposit()
        );
    }

    private boolean hasAssetSnapshot(BehaviorEvent event) {
        return event.getCurrentCash() != null
                && event.getCurrentStockPrincipal() != null
                && event.getCurrentDeposit() != null;
    }

    private BigDecimal calculateHoldingDays(
            BehaviorEvent currentEvent,
            List<BehaviorEvent> previousEvents) {
        if (currentEvent.getActionType() != BehaviorActionType.SELL
                || currentEvent.getSecurityId() == null
                || currentEvent.getTradedAt() == null) {
            return null;
        }

        return previousEvents.stream()
                .filter(event -> event.getActionType() == BehaviorActionType.BUY)
                .filter(event -> currentEvent.getSecurityId().equals(event.getSecurityId()))
                .filter(event -> event.getTradedAt() != null)
                .map(BehaviorEvent::getTradedAt)
                .min(LocalDateTime::compareTo)
                .map(boughtAt -> BigDecimal.valueOf(
                        ChronoUnit.DAYS.between(boughtAt, currentEvent.getTradedAt())
                ))
                .orElse(null);
    }

    private int calculateConsecutiveActionCount(
            BehaviorEvent currentEvent,
            List<BehaviorEvent> previousEvents,
            MarketState marketState) {
        if (currentEvent.getActionType() != BehaviorActionType.BUY
                && currentEvent.getActionType() != BehaviorActionType.SELL) {
            return 1;
        }

        int consecutiveActionCount = 1;
        for (BehaviorEvent previousEvent : previousEvents) {
            if (previousEvent.getActionType() != currentEvent.getActionType()
                    || previousEvent.getMarketState() != marketState
                    || !isRelatedActionTime(previousEvent.getTradedAt(), currentEvent.getTradedAt())) {
                break;
            }
            consecutiveActionCount++;
        }
        return consecutiveActionCount;
    }

    private boolean isRelatedActionTime(LocalDateTime previousAt, LocalDateTime currentAt) {
        if (previousAt == null || currentAt == null || previousAt.isAfter(currentAt)) {
            return false;
        }
        return Duration.between(previousAt, currentAt).toHours() <= RELATED_ACTION_HOURS;
    }
}
