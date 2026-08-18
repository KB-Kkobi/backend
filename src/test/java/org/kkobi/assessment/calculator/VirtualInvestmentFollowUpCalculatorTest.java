package org.kkobi.assessment.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.dto.AccountDailySnapshotDto;
import org.kkobi.assessment.dto.VirtualInvestmentBehaviorDto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualInvestmentFollowUpCalculatorTest {

    private static final LocalDate ASSESSMENT_DATE = LocalDate.of(2026, 8, 12);

    private final VirtualInvestmentFollowUpCalculator calculator =
            new VirtualInvestmentFollowUpCalculator(new MarketStateCalculator());

    @Test
    @DisplayName("예금 해지 후 48시간 내 매수와 2일 현금 유지를 구분한다.")
    void calculateDepositFollowUpRules() {
        VirtualInvestmentBehaviorDto cancel = behavior(
                "TERMINATE", null, null, "2026-08-10T09:00:00"
        );
        VirtualInvestmentBehaviorDto buy = behavior(
                "BUY", 1L, 10, "2026-08-12T08:00:00"
        );

        BehaviorContext buyContext = calculator.calculateFollowUpContext(
                List.of(cancel, buy),
                List.of(snapshot("2026-08-10", 100L, "50"),
                        snapshot("2026-08-11", 100L, "50"),
                        snapshot("2026-08-12", 100L, "50")),
                ASSESSMENT_DATE
        );
        BehaviorContext cashContext = calculator.calculateFollowUpContext(
                List.of(cancel),
                List.of(snapshot("2026-08-10", 100L, "50"),
                        snapshot("2026-08-11", 90L, "50"),
                        snapshot("2026-08-12", 85L, "50")),
                ASSESSMENT_DATE
        );

        assertTrue(buyContext.isDepositCancelledBeforeSecurityBuy());
        assertFalse(buyContext.isDepositCancelCashRetention());
        assertTrue(cashContext.isDepositCancelCashRetention());
    }

    @Test
    @DisplayName("정상장 20~50% 부분 매도 후 현금 비중을 이틀 유지한다.")
    void calculateNormalPartialSellCashRetention() {
        VirtualInvestmentBehaviorDto buy = behavior(
                "BUY", 1L, 100, "2026-08-01T10:00:00"
        );
        buy.setExecutionPrice(100L);
        VirtualInvestmentBehaviorDto sell = behavior(
                "SELL", 1L, 30, "2026-08-10T10:00:00"
        );
        sell.setExecutionPrice(101L);
        setNormalMarketPrices(sell);

        BehaviorContext context = calculator.calculateFollowUpContext(
                List.of(buy, sell),
                List.of(snapshot("2026-08-11", 30L, "30"),
                        snapshot("2026-08-12", 30L, "30")),
                ASSESSMENT_DATE
        );

        assertTrue(context.isNormalPartialSellCashRetention());
    }

    @Test
    @DisplayName("유동성을 남긴 매수 후 10일 안에 수익 매도하면 기회 실행을 완료한다.")
    void calculateCompletedLiquidityOpportunity() {
        VirtualInvestmentBehaviorDto buy = behavior(
                "BUY", 1L, 10, "2026-08-05T10:00:00"
        );
        buy.setExecutionPrice(100L);
        VirtualInvestmentBehaviorDto sell = behavior(
                "SELL", 1L, 10, "2026-08-12T10:00:00"
        );
        sell.setExecutionPrice(110L);

        BehaviorContext context = calculator.calculateFollowUpContext(
                List.of(buy, sell),
                List.of(snapshot("2026-08-05", 30L, "30")),
                ASSESSMENT_DATE
        );

        assertTrue(context.isCompletedLiquidityOpportunity());
    }

    @Test
    @DisplayName("LHH 수익 매도는 개별 매수가가 아닌 보유 평균 매입단가로 판정한다.")
    void calculateLiquidityOpportunityUsingAveragePurchasePrice() {
        VirtualInvestmentBehaviorDto firstBuy = behavior(
                "BUY", 1L, 10, "2026-08-05T10:00:00"
        );
        firstBuy.setExecutionPrice(100L);
        VirtualInvestmentBehaviorDto secondBuy = behavior(
                "BUY", 1L, 10, "2026-08-06T10:00:00"
        );
        secondBuy.setExecutionPrice(200L);
        VirtualInvestmentBehaviorDto breakEvenSell = behavior(
                "SELL", 1L, 10, "2026-08-12T10:00:00"
        );
        breakEvenSell.setExecutionPrice(150L);
        VirtualInvestmentBehaviorDto profitSell = behavior(
                "SELL", 1L, 10, "2026-08-12T10:00:00"
        );
        profitSell.setExecutionPrice(160L);
        List<AccountDailySnapshotDto> snapshots = List.of(
                snapshot("2026-08-05", 30L, "30"),
                snapshot("2026-08-06", 30L, "30")
        );

        BehaviorContext breakEvenContext = calculator.calculateFollowUpContext(
                List.of(firstBuy, secondBuy, breakEvenSell),
                snapshots,
                ASSESSMENT_DATE
        );
        BehaviorContext profitContext = calculator.calculateFollowUpContext(
                List.of(firstBuy, secondBuy, profitSell),
                snapshots,
                ASSESSMENT_DATE
        );

        assertFalse(breakEvenContext.isCompletedLiquidityOpportunity());
        assertTrue(profitContext.isCompletedLiquidityOpportunity());
    }

    private VirtualInvestmentBehaviorDto behavior(
            String actionType,
            Long securityId,
            Integer quantity,
            String tradedAt) {
        VirtualInvestmentBehaviorDto behavior = new VirtualInvestmentBehaviorDto();
        behavior.setActionType(actionType);
        behavior.setSecurityId(securityId);
        behavior.setQuantity(quantity);
        behavior.setTradedAt(Timestamp.valueOf(LocalDateTime.parse(tradedAt)));
        return behavior;
    }

    private AccountDailySnapshotDto snapshot(
            String snapshotDate,
            long currentCash,
            String cashRatio) {
        AccountDailySnapshotDto snapshot = new AccountDailySnapshotDto();
        snapshot.setSnapshotDate(LocalDate.parse(snapshotDate));
        snapshot.setCurrentCash(currentCash);
        snapshot.setCashRatio(new BigDecimal(cashRatio));
        return snapshot;
    }

    private void setNormalMarketPrices(VirtualInvestmentBehaviorDto behavior) {
        behavior.setPreviousClosePrice(BigDecimal.valueOf(100));
        behavior.setCurrentClosePrice(BigDecimal.valueOf(101));
        behavior.setOpenPrice(BigDecimal.valueOf(100));
        behavior.setHighPrice(BigDecimal.valueOf(102));
        behavior.setLowPrice(BigDecimal.valueOf(99));
    }
}
