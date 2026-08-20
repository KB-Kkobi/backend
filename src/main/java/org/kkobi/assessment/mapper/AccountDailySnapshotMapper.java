package org.kkobi.assessment.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.assessment.dto.AccountDailySnapshotDto;

import java.time.LocalDate;
import java.util.List;

public interface AccountDailySnapshotMapper {

    List<AccountDailySnapshotDto> getAccountSnapshotTargets(
            @Param("snapshotDate") LocalDate snapshotDate
    );

    List<AccountDailySnapshotDto> getUnsettledDailyAssessmentTargets(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    int saveAccountDailySnapshot(AccountDailySnapshotDto accountDailySnapshot);

    List<AccountDailySnapshotDto> getAccountDailySnapshots(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    LocalDate getFirstCompletedSecurityOrderDate(@Param("accountId") Long accountId);

    int getCompletedSecurityOrderCount(
            @Param("accountId") Long accountId,
            @Param("assessmentDate") LocalDate assessmentDate
    );

    int getCompletedSecurityOrderCountBetween(
            @Param("accountId") Long accountId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

}
