package org.kkobi.assessment.scheduler;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.service.AccountDailySnapshotService;
import org.kkobi.assessment.service.VirtualInvestmentPeriodAssessmentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
@RequiredArgsConstructor
public class VirtualInvestmentAssessmentScheduler {

    private static final String ASIA_SEOUL = "Asia/Seoul";
    private static final ZoneId ASIA_SEOUL_ZONE = ZoneId.of(ASIA_SEOUL);

    private final AccountDailySnapshotService accountDailySnapshotService;
    private final VirtualInvestmentPeriodAssessmentService virtualInvestmentPeriodAssessmentService;

    @Scheduled(cron = "0 0 0 * * *", zone = ASIA_SEOUL)
    public void calculateDailyVirtualInvestmentAssessment() {
        LocalDate assessmentDate = LocalDate.now(ASIA_SEOUL_ZONE).minusDays(1);
        accountDailySnapshotService.saveAccountDailySnapshots(assessmentDate);
        virtualInvestmentPeriodAssessmentService.calculateDailyAssessments(assessmentDate);
    }

    @Scheduled(cron = "0 10 0 * * SUN", zone = ASIA_SEOUL)
    public void calculateWeeklyVirtualInvestmentAssessment() {
        LocalDate assessmentDate = LocalDate.now(ASIA_SEOUL_ZONE);
        virtualInvestmentPeriodAssessmentService.calculateWeeklyCashAssessments(assessmentDate);
        virtualInvestmentPeriodAssessmentService.calculateWeeklyTradeFrequencyAssessments(
                assessmentDate
        );
        virtualInvestmentPeriodAssessmentService.calculateWeeklyBalanceAssessments(assessmentDate);
    }
}
