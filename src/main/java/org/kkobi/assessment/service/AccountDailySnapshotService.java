package org.kkobi.assessment.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.calculator.AssetRatioCalculator;
import org.kkobi.assessment.dto.AccountDailySnapshotDto;
import org.kkobi.assessment.mapper.AccountDailySnapshotMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountDailySnapshotService {

    private final AccountDailySnapshotMapper accountDailySnapshotMapper;
    private final AssetRatioCalculator assetRatioCalculator;

    @Transactional
    public int saveAccountDailySnapshots(LocalDate snapshotDate) {
        List<AccountDailySnapshotDto> snapshotTargets = accountDailySnapshotMapper
                .getAccountSnapshotTargets(snapshotDate);
        return snapshotTargets.stream()
                .map(snapshotTarget -> createAccountDailySnapshot(
                        snapshotTarget.getAccountId(),
                        snapshotDate,
                        snapshotTarget.getCurrentCash(),
                        snapshotTarget.getCurrentStockPrincipal(),
                        snapshotTarget.getCurrentDeposit()
                ))
                .mapToInt(this::saveAccountDailySnapshot)
                .sum();
    }

    private int saveAccountDailySnapshot(AccountDailySnapshotDto accountDailySnapshot) {
        int savedRowCount = accountDailySnapshotMapper.saveAccountDailySnapshot(accountDailySnapshot);
        if (savedRowCount < 0 || savedRowCount > 2) {
            throw new IllegalStateException("가상투자 일별 자산 스냅샷을 저장하지 못했습니다.");
        }
        return savedRowCount == 0 ? 0 : 1;
    }

    private AccountDailySnapshotDto createAccountDailySnapshot(
            Long accountId,
            LocalDate snapshotDate,
            Long currentCash,
            Long currentStockPrincipal,
            Long currentDeposit) {
        AccountDailySnapshotDto snapshot = new AccountDailySnapshotDto();
        snapshot.setAccountId(accountId);
        snapshot.setSnapshotDate(snapshotDate);
        snapshot.setCurrentCash(currentCash);
        snapshot.setCurrentStockPrincipal(currentStockPrincipal);
        snapshot.setCurrentDeposit(currentDeposit);
        snapshot.setTotalInvestedPrincipal(
                Math.addExact(Math.addExact(currentCash, currentStockPrincipal), currentDeposit)
        );
        snapshot.setCashRatio(assetRatioCalculator.calculateCashRatio(
                currentCash,
                currentStockPrincipal,
                currentDeposit
        ));
        return snapshot;
    }
}
