package org.kkobi.assessment.calculator;

import org.kkobi.assessment.domain.BehaviorEvent;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

@Component
public class SecurityPositionCalculator {

    private static final BigDecimal PERCENTAGE = BigDecimal.valueOf(100);
    private static final int RATE_SCALE = 2;
    private static final int PRICE_SCALE = 8;

    public BigDecimal calculatePositionReturnRate(
            BehaviorEvent currentEvent,
            List<BehaviorEvent> previousEvents) {
        if (currentEvent.getActionType() != BehaviorActionType.BUY
                || currentEvent.getAssetType() != BehaviorAssetType.SECURITY
                || currentEvent.getSecurityId() == null
                || !existsTradePrice(currentEvent)) {
            return null;
        }

        SecurityPosition securityPosition = calculatePreviousSecurityPosition(
                currentEvent.getSecurityId(),
                previousEvents
        );
        if (securityPosition.quantity() == 0 || securityPosition.averagePrice().signum() == 0) {
            return null;
        }

        BigDecimal currentPrice = calculateTradePrice(currentEvent);
        return currentPrice.subtract(securityPosition.averagePrice())
                .multiply(PERCENTAGE)
                .divide(securityPosition.averagePrice(), RATE_SCALE, RoundingMode.HALF_UP);
    }

    public int calculateCurrentSecurityQuantity(
            BehaviorEvent currentEvent,
            List<BehaviorEvent> previousEvents) {
        SecurityPosition previousPosition = calculatePreviousSecurityPosition(
                currentEvent.getSecurityId(),
                previousEvents
        );
        if (currentEvent.getQuantity() == null) {
            return previousPosition.quantity();
        }
        if (currentEvent.getActionType() == BehaviorActionType.BUY) {
            return previousPosition.quantity() + currentEvent.getQuantity();
        }
        if (currentEvent.getActionType() == BehaviorActionType.SELL) {
            return Math.max(0, previousPosition.quantity() - currentEvent.getQuantity());
        }
        return previousPosition.quantity();
    }

    private SecurityPosition calculatePreviousSecurityPosition(
            Long securityId,
            List<BehaviorEvent> previousEvents) {
        if (securityId == null) {
            return new SecurityPosition(0, BigDecimal.ZERO);
        }

        int quantity = 0;
        BigDecimal averagePrice = BigDecimal.ZERO;
        List<BehaviorEvent> securityEvents = previousEvents.stream()
                .filter(event -> event.getAssetType() == BehaviorAssetType.SECURITY)
                .filter(event -> securityId.equals(event.getSecurityId()))
                .filter(this::existsTradePrice)
                .filter(event -> event.getTradedAt() != null)
                .sorted(Comparator.comparing(BehaviorEvent::getTradedAt))
                .toList();

        for (BehaviorEvent securityEvent : securityEvents) {
            if (securityEvent.getActionType() == BehaviorActionType.BUY) {
                BigDecimal previousPrincipal = averagePrice.multiply(BigDecimal.valueOf(quantity));
                BigDecimal tradePrincipal = calculateTradePrice(securityEvent)
                        .multiply(BigDecimal.valueOf(securityEvent.getQuantity()));
                quantity += securityEvent.getQuantity();
                averagePrice = previousPrincipal.add(tradePrincipal)
                        .divide(BigDecimal.valueOf(quantity), PRICE_SCALE, RoundingMode.HALF_UP);
            } else if (securityEvent.getActionType() == BehaviorActionType.SELL) {
                quantity = Math.max(0, quantity - securityEvent.getQuantity());
                if (quantity == 0) {
                    averagePrice = BigDecimal.ZERO;
                }
            }
        }

        return new SecurityPosition(quantity, averagePrice);
    }

    private boolean existsTradePrice(BehaviorEvent event) {
        if (event.getQuantity() == null || event.getQuantity() <= 0) {
            return false;
        }

        return event.getExecutionPrice() != null && event.getExecutionPrice() > 0
                || event.getActionAmount() != null && event.getActionAmount() > 0;
    }

    private BigDecimal calculateTradePrice(BehaviorEvent event) {
        if (event.getExecutionPrice() != null) {
            return BigDecimal.valueOf(event.getExecutionPrice());
        }

        return BigDecimal.valueOf(event.getActionAmount())
                .divide(BigDecimal.valueOf(event.getQuantity()), PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private record SecurityPosition(int quantity, BigDecimal averagePrice) {
    }
}
