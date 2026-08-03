package org.kkobi.assessment.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.domain.BehaviorEvent;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecurityPositionCalculatorTest {

    private final SecurityPositionCalculator securityPositionCalculator =
            new SecurityPositionCalculator();

    @Test
    @DisplayName("과거 매매 내역으로 추가 매수 전 평균 단가와 보유 수량을 계산한다.")
    void calculateSecurityPosition() {
        BehaviorEvent firstBuy = createSecurityEvent(
                BehaviorActionType.BUY,
                10,
                1_000L,
                LocalDateTime.of(2026, 8, 1, 9, 0)
        );
        BehaviorEvent partialSell = createSecurityEvent(
                BehaviorActionType.SELL,
                4,
                440L,
                LocalDateTime.of(2026, 8, 2, 9, 0)
        );
        BehaviorEvent currentBuy = createSecurityEvent(
                BehaviorActionType.BUY,
                2,
                160L,
                LocalDateTime.of(2026, 8, 3, 9, 0)
        );

        BigDecimal positionReturnRate = securityPositionCalculator.calculatePositionReturnRate(
                currentBuy,
                List.of(firstBuy, partialSell)
        );
        int currentQuantity = securityPositionCalculator.calculateCurrentSecurityQuantity(
                currentBuy,
                List.of(firstBuy, partialSell)
        );

        assertEquals(0, new BigDecimal("-20.00").compareTo(positionReturnRate));
        assertEquals(8, currentQuantity);
    }

    private BehaviorEvent createSecurityEvent(
            BehaviorActionType actionType,
            int quantity,
            long actionAmount,
            LocalDateTime tradedAt) {
        BehaviorEvent event = new BehaviorEvent();
        event.setActionType(actionType);
        event.setAssetType(BehaviorAssetType.SECURITY);
        event.setSecurityId(1L);
        event.setQuantity(quantity);
        event.setActionAmount(actionAmount);
        event.setTradedAt(tradedAt);
        return event;
    }
}
