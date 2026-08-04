package org.kkobi.assessment.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.domain.BehaviorEvent;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BehaviorContextFactoryTest {

    private final BehaviorContextFactory behaviorContextFactory = new BehaviorContextFactory(
            new AssetRatioCalculator(),
            new MarketStateCalculator()
    );

    @Test
    @DisplayName("단일 종목의 같은 tick 매매와 tick 간 보유 기간을 계산한다.")
    void createBehaviorContextCalculatesSingleSecurityBehavior() {
        LocalDateTime boughtAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        BehaviorEvent buyEvent = createSecurityEvent(boughtAt);
        BehaviorEvent sameTickSellEvent = createSecurityEvent(boughtAt);
        sameTickSellEvent.setActionType(BehaviorActionType.SELL);
        sameTickSellEvent.setCurrentStockPrincipal(0L);

        BehaviorContext sameTickContext = behaviorContextFactory.createBehaviorContext(
                sameTickSellEvent,
                List.of(buyEvent)
        );

        assertTrue(sameTickContext.isSameDayTrade());
        assertEquals(0, BigDecimal.ZERO.compareTo(sameTickContext.getAverageHoldingDays()));

        BehaviorEvent laterSellEvent = createSecurityEvent(boughtAt.plusDays(14));
        laterSellEvent.setActionType(BehaviorActionType.SELL);
        laterSellEvent.setCurrentStockPrincipal(0L);
        BehaviorContext holdingContext = behaviorContextFactory.createBehaviorContext(
                laterSellEvent,
                List.of(buyEvent)
        );

        assertEquals(0, new BigDecimal("14").compareTo(holdingContext.getAverageHoldingDays()));

        BehaviorEvent previousSellEvent = createSecurityEvent(boughtAt);
        previousSellEvent.setActionType(BehaviorActionType.SELL);
        previousSellEvent.setCurrentStockPrincipal(0L);
        BehaviorEvent sameSecurityBuyEvent = createSecurityEvent(boughtAt.plusHours(1));
        BehaviorContext sameSecurityContext = behaviorContextFactory.createBehaviorContext(
                sameSecurityBuyEvent,
                List.of(previousSellEvent)
        );

        assertFalse(sameSecurityContext.isStockRotation());
    }

    @Test
    @DisplayName("부분 매도 이후 전량 매도할 때 최초 보유 tick부터 기간을 계산한다.")
    void createBehaviorContextMaintainsHoldingPeriodAfterPartialSell() {
        LocalDateTime boughtAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        BehaviorEvent buyEvent = createSecurityEvent(boughtAt);
        BehaviorEvent partialSellEvent = createSecurityEvent(boughtAt.plusDays(7));
        partialSellEvent.setActionType(BehaviorActionType.SELL);
        partialSellEvent.setCurrentStockPrincipal(400_000L);
        BehaviorEvent fullSellEvent = createSecurityEvent(boughtAt.plusDays(14));
        fullSellEvent.setActionType(BehaviorActionType.SELL);
        fullSellEvent.setCurrentStockPrincipal(0L);

        BehaviorContext context = behaviorContextFactory.createBehaviorContext(
                fullSellEvent,
                List.of(buyEvent, partialSellEvent)
        );

        assertEquals(0, new BigDecimal("14").compareTo(context.getAverageHoldingDays()));
    }

    @Test
    @DisplayName("전량 매도 후 재매수한 종목은 최근 보유 구간의 최초 매수일부터 계산한다.")
    void createBehaviorContextCalculatesReopenedHoldingPeriod() {
        BehaviorEvent firstBuy = createVirtualSecurityEvent(
                BehaviorActionType.BUY,
                10,
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        BehaviorEvent firstFullSell = createVirtualSecurityEvent(
                BehaviorActionType.SELL,
                10,
                LocalDateTime.of(2026, 1, 5, 0, 0)
        );
        BehaviorEvent reopenedBuy = createVirtualSecurityEvent(
                BehaviorActionType.BUY,
                5,
                LocalDateTime.of(2026, 1, 20, 0, 0)
        );
        BehaviorEvent currentFullSell = createVirtualSecurityEvent(
                BehaviorActionType.SELL,
                5,
                LocalDateTime.of(2026, 1, 30, 0, 0)
        );
        currentFullSell.setCurrentSecurityQuantity(0);

        BehaviorContext context = behaviorContextFactory.createBehaviorContext(
                currentFullSell,
                List.of(firstBuy, firstFullSell, reopenedBuy)
        );

        assertEquals(0, new BigDecimal("10").compareTo(context.getAverageHoldingDays()));
    }

    private BehaviorEvent createSecurityEvent(LocalDateTime tradedAt) {
        BehaviorEvent event = new BehaviorEvent();
        event.setActionType(BehaviorActionType.BUY);
        event.setAssetType(BehaviorAssetType.SECURITY);
        event.setSecurityId(1L);
        event.setCurrentCash(100_000L);
        event.setCurrentStockPrincipal(800_000L);
        event.setCurrentDeposit(100_000L);
        event.setTradedAt(tradedAt);
        return event;
    }

    private BehaviorEvent createVirtualSecurityEvent(
            BehaviorActionType actionType,
            int quantity,
            LocalDateTime tradedAt) {
        BehaviorEvent event = new BehaviorEvent();
        event.setActionType(actionType);
        event.setAssetType(BehaviorAssetType.SECURITY);
        event.setSecurityId(1L);
        event.setQuantity(quantity);
        event.setTradedAt(tradedAt);
        return event;
    }
}
