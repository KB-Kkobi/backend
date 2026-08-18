package org.kkobi.assessment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.calculator.AssetRatioCalculator;
import org.kkobi.assessment.dto.AccountDailySnapshotDto;
import org.kkobi.assessment.mapper.AccountDailySnapshotMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountDailySnapshotServiceTest {

    @Test
    @DisplayName("과거 누락 날짜를 소급하지 않고 요청한 날짜의 스냅샷만 저장한다.")
    void saveAccountDailySnapshotForRequestedDate() {
        InMemoryAccountDailySnapshotMapper snapshotMapper =
                new InMemoryAccountDailySnapshotMapper();
        AccountDailySnapshotService snapshotService = new AccountDailySnapshotService(
                snapshotMapper,
                new AssetRatioCalculator()
        );

        int savedSnapshotCount = snapshotService.saveAccountDailySnapshots(
                LocalDate.of(2026, 8, 3)
        );

        assertEquals(1, savedSnapshotCount);
        assertEquals(
                List.of(LocalDate.of(2026, 8, 3)),
                snapshotMapper.getSavedSnapshots().stream()
                        .map(AccountDailySnapshotDto::getSnapshotDate)
                        .toList()
        );
        AccountDailySnapshotDto savedSnapshot = snapshotMapper.getSavedSnapshots().get(0);
        assertEquals(100L, savedSnapshot.getTotalInvestedPrincipal());
        assertEquals(0, new BigDecimal("20.00").compareTo(savedSnapshot.getCashRatio()));
    }

    private static class InMemoryAccountDailySnapshotMapper
            implements AccountDailySnapshotMapper {

        private final List<AccountDailySnapshotDto> savedSnapshots = new ArrayList<>();

        @Override
        public List<AccountDailySnapshotDto> getAccountSnapshotTargets(LocalDate snapshotDate) {
            AccountDailySnapshotDto snapshotTarget = new AccountDailySnapshotDto();
            snapshotTarget.setAccountId(1L);
            snapshotTarget.setCurrentCash(20L);
            snapshotTarget.setCurrentStockPrincipal(70L);
            snapshotTarget.setCurrentDeposit(10L);
            return List.of(snapshotTarget);
        }

        @Override
        public int saveAccountDailySnapshot(AccountDailySnapshotDto accountDailySnapshot) {
            savedSnapshots.add(accountDailySnapshot);
            return 1;
        }

        @Override
        public List<AccountDailySnapshotDto> getAccountDailySnapshots(
                Long accountId,
                LocalDate startDate,
                LocalDate endDate) {
            return List.of();
        }

        @Override
        public LocalDate getFirstCompletedSecurityOrderDate(Long accountId) {
            return null;
        }

        @Override
        public int getCompletedSecurityOrderCount(Long accountId, LocalDate assessmentDate) {
            return 0;
        }

        @Override
        public int getCompletedSecurityOrderCountBetween(
                Long accountId,
                LocalDate startDate,
                LocalDate endDate) {
            return 0;
        }

        private List<AccountDailySnapshotDto> getSavedSnapshots() {
            return savedSnapshots;
        }
    }
}
