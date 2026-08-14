package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.enums.BehaviorActionType;
import org.kkobi.assessment.enums.BehaviorAssetType;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimulatedGamePortfolioTest {

    @Test
    @DisplayName("주식을 매수하면 현금과 평균 매입가를 갱신한다.")
    void buyStock() {
        SimulatedGamePortfolio portfolio = createPortfolio();

        SimulatedGameAction action = portfolio.buyStock(17, 50, 10_000L);

        assertEquals(4_500_000L, portfolio.getCurrentCash());
        assertEquals(2_500_000L, portfolio.getCurrentStockPrincipal());
        assertEquals(150, portfolio.getCurrentStockQuantity());
        assertEquals(
                new BigDecimal("16666.66666667"),
                portfolio.getAveragePurchasePrice()
        );
        assertEquals(BehaviorActionType.BUY, action.getActionType());
        assertEquals(BehaviorAssetType.SECURITY, action.getAssetType());
        assertEquals(new BigDecimal("-50.00"), action.getPositionReturnRate());
        assertNull(action.getRealizedReturnRate());
    }

    @Test
    @DisplayName("주식을 일부 매도하면 실현 손익률과 행동 후 자산을 계산한다.")
    void sellStock() {
        SimulatedGamePortfolio portfolio = createPortfolio();

        SimulatedGameAction action = portfolio.sellStock(30, 40, 25_000L);

        assertEquals(6_000_000L, portfolio.getCurrentCash());
        assertEquals(1_200_000L, portfolio.getCurrentStockPrincipal());
        assertEquals(60, portfolio.getCurrentStockQuantity());
        assertEquals(new BigDecimal("20000.00000000"), portfolio.getAveragePurchasePrice());
        assertEquals(new BigDecimal("25.00"), action.getRealizedReturnRate());
        assertNull(action.getPositionReturnRate());
    }

    @Test
    @DisplayName("보유 현금과 주식 수량을 초과하는 거래를 차단한다.")
    void rejectInvalidSecurityAction() {
        SimulatedGamePortfolio portfolio = createPortfolio();

        assertThrows(
                IllegalStateException.class,
                () -> portfolio.buyStock(1, 501, 10_000L)
        );
        assertThrows(
                IllegalStateException.class,
                () -> portfolio.sellStock(1, 101, 20_000L)
        );
    }

    @Test
    @DisplayName("예금을 중도 해지하면 원금을 현금으로 이동한다.")
    void cancelDeposit() {
        SimulatedGamePortfolio portfolio = createPortfolio();

        SimulatedGameAction action = portfolio.cancelDeposit(20);

        assertEquals(8_000_000L, portfolio.getCurrentCash());
        assertEquals(0L, portfolio.getCurrentDeposit());
        assertTrue(portfolio.isDepositCancelled());
        assertEquals(BehaviorActionType.CANCEL_PRODUCT, action.getActionType());
        assertEquals(3_000_000L, action.getActionAmount());
    }

    @Test
    @DisplayName("게임 종료 시 예금 원금과 이자를 만기 처리한다.")
    void matureDeposit() {
        SimulatedGamePortfolio portfolio = createPortfolio();

        SimulatedGameAction action = portfolio.matureDeposit(52, 100_000L);

        assertEquals(8_100_000L, portfolio.getCurrentCash());
        assertEquals(0L, portfolio.getCurrentDeposit());
        assertTrue(portfolio.isDepositMatured());
        assertEquals(BehaviorActionType.MATURITY, action.getActionType());
        assertEquals(3_100_000L, action.getActionAmount());
    }

    @Test
    @DisplayName("예금 만기 전 처리와 종료된 예금의 재처리를 차단한다.")
    void rejectInvalidDepositAction() {
        SimulatedGamePortfolio portfolio = createPortfolio();

        assertThrows(
                IllegalArgumentException.class,
                () -> portfolio.matureDeposit(51, 100_000L)
        );

        portfolio.cancelDeposit(20);

        assertThrows(
                IllegalStateException.class,
                () -> portfolio.matureDeposit(52, 100_000L)
        );
    }

    private SimulatedGamePortfolio createPortfolio() {
        return new SimulatedGamePortfolio(
                5_000_000L,
                2_000_000L,
                3_000_000L,
                100
        );
    }
}
