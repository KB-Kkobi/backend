package org.kkobi.assessment.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.dto.AccountDailySnapshotDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class VirtualInvestmentPeriodCalculatorTest {

    private final VirtualInvestmentPeriodCalculator calculator =
            new VirtualInvestmentPeriodCalculator(new AssetRatioCalculator());

    @Test
    @DisplayName("7일 스냅샷의 평균 주식 비중과 평균 현금 비중을 계산한다.")
    void calculateSevenDayAverageRatios() {
        List<AccountDailySnapshotDto> snapshots = createSnapshots(7, 10L, 80L, 10L);

        BigDecimal averageStockRatio = calculator.calculateAverageStockRatio(snapshots);
        BigDecimal averageCashRatio = calculator.calculateAverageCashRatio(snapshots);

        assertEquals(0, new BigDecimal("80.00").compareTo(averageStockRatio));
        assertEquals(0, new BigDecimal("10.00").compareTo(averageCashRatio));
    }

    @Test
    @DisplayName("5일 동안 같은 현금 비중 구간을 유지했는지 계산한다.")
    void calculateMaintainedCashRatio() {
        List<AccountDailySnapshotDto> maintainedSnapshots = createSnapshots(5, 4L, 96L, 0L);
        List<AccountDailySnapshotDto> changedSnapshots = createSnapshots(4, 4L, 96L, 0L);
        changedSnapshots.add(createSnapshot(4, 10L, 90L, 0L));

        BigDecimal maintainedCashRatio = calculator.calculateMaintainedCashRatio(
                maintainedSnapshots
        );
        BigDecimal changedCashRatio = calculator.calculateMaintainedCashRatio(changedSnapshots);

        assertEquals(0, new BigDecimal("4.00").compareTo(maintainedCashRatio));
        assertNull(changedCashRatio);
    }

    @Test
    @DisplayName("가상투자 시작일부터 평가일까지의 일평균 거래 횟수를 계산한다.")
    void calculateAverageDailyTradeCount() {
        BigDecimal averageDailyTradeCount = calculator.calculateAverageDailyTradeCount(
                1,
                LocalDate.of(2026, 8, 1),
                LocalDate.of(2026, 8, 6)
        );

        assertEquals(0, new BigDecimal("0.2000").compareTo(averageDailyTradeCount));
    }

    private List<AccountDailySnapshotDto> createSnapshots(
            int count,
            long currentCash,
            long currentStockPrincipal,
            long currentDeposit) {
        return IntStream.range(0, count)
                .mapToObj(dayOffset -> createSnapshot(
                        dayOffset,
                        currentCash,
                        currentStockPrincipal,
                        currentDeposit
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    private AccountDailySnapshotDto createSnapshot(
            int dayOffset,
            long currentCash,
            long currentStockPrincipal,
            long currentDeposit) {
        AccountDailySnapshotDto snapshot = new AccountDailySnapshotDto();
        snapshot.setSnapshotDate(LocalDate.of(2026, 8, 1).plusDays(dayOffset));
        snapshot.setCurrentCash(currentCash);
        snapshot.setCurrentStockPrincipal(currentStockPrincipal);
        snapshot.setCurrentDeposit(currentDeposit);
        snapshot.setCashRatio(BigDecimal.valueOf(currentCash));
        return snapshot;
    }
}
