package org.kkobi.assessment.scheduler;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.service.AccountDailySnapshotService;
import org.kkobi.assessment.service.VirtualInvestmentPeriodAssessmentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class VirtualInvestmentAssessmentScheduler {

    private static final String ASIA_SEOUL = "Asia/Seoul";
    private static final ZoneId ASIA_SEOUL_ZONE = ZoneId.of(ASIA_SEOUL);
    private static final int DAILY_CATCH_UP_DAYS = 14;

    private final AccountDailySnapshotService accountDailySnapshotService;
    private final VirtualInvestmentPeriodAssessmentService virtualInvestmentPeriodAssessmentService;

    @Scheduled(cron = "0 0 0 * * *", zone = ASIA_SEOUL)
    public void calculateDailyVirtualInvestmentAssessment() {
        LocalDate assessmentDate = LocalDate.now(ASIA_SEOUL_ZONE).minusDays(1);
        accountDailySnapshotService.saveAccountDailySnapshots(assessmentDate);
        List<LocalDate> unsettledDates = virtualInvestmentPeriodAssessmentService
                .getUnsettledDailyAssessmentDates(
                        assessmentDate.minusDays(DAILY_CATCH_UP_DAYS - 1L),
                        assessmentDate
                );
        unsettledDates.forEach(
                virtualInvestmentPeriodAssessmentService::calculateDailyAssessments
        );
    }

    @Scheduled(cron = "0 10 0 * * SUN", zone = ASIA_SEOUL)
    public void calculateWeeklyVirtualInvestmentAssessment() {
        LocalDate assessmentDate = LocalDate.now(ASIA_SEOUL_ZONE);
        virtualInvestmentPeriodAssessmentService.calculateWeeklyCashAssessments(assessmentDate);
        virtualInvestmentPeriodAssessmentService.calculateWeeklyTradeFrequencyAssessments(
                assessmentDate
        );
    }
}
