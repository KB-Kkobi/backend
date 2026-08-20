package org.kkobi.assessment.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.dto.AccountDailySnapshotDto;
import org.kkobi.assessment.service.AccountDailySnapshotService;
import org.kkobi.assessment.service.VirtualInvestmentPeriodAssessmentService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VirtualInvestmentAssessmentSchedulerTest {

    private static final ZoneId ASIA_SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Test
    @DisplayName("한 날짜의 계정 재정산이 실패해도 다음 날짜와 계정을 계속 처리한다.")
    void continueDailyCatchUpAfterAccountFailure() {
        AccountDailySnapshotService snapshotService = mock(AccountDailySnapshotService.class);
        VirtualInvestmentPeriodAssessmentService assessmentService =
                mock(VirtualInvestmentPeriodAssessmentService.class);
        VirtualInvestmentAssessmentScheduler scheduler =
                new VirtualInvestmentAssessmentScheduler(snapshotService, assessmentService);
        LocalDate assessmentDate = LocalDate.now(ASIA_SEOUL_ZONE).minusDays(1);
        AccountDailySnapshotDto failedTarget = target(1L, assessmentDate.minusDays(1));
        AccountDailySnapshotDto succeededTarget = target(2L, assessmentDate);
        when(assessmentService.getUnsettledDailyAssessmentTargets(any(), any()))
                .thenReturn(List.of(failedTarget, succeededTarget));
        when(assessmentService.calculateDailyAssessment(failedTarget))
                .thenThrow(new IllegalStateException("정산 실패"));
        when(assessmentService.calculateDailyAssessment(succeededTarget)).thenReturn(1);

        scheduler.calculateDailyVirtualInvestmentAssessment();

        verify(snapshotService).saveAccountDailySnapshots(assessmentDate);
        verify(assessmentService).calculateDailyAssessment(failedTarget);
        verify(assessmentService).calculateDailyAssessment(succeededTarget);
    }

    private AccountDailySnapshotDto target(Long accountId, LocalDate snapshotDate) {
        AccountDailySnapshotDto target = new AccountDailySnapshotDto();
        target.setAccountId(accountId);
        target.setSnapshotDate(snapshotDate);
        return target;
    }
}
