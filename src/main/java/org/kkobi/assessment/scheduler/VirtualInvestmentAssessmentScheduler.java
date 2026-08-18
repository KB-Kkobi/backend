package org.kkobi.assessment.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kkobi.assessment.dto.AccountDailySnapshotDto;
import org.kkobi.assessment.service.AccountDailySnapshotService;
import org.kkobi.assessment.service.VirtualInvestmentPeriodAssessmentService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
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
        List<AccountDailySnapshotDto> unsettledTargets = virtualInvestmentPeriodAssessmentService
                .getUnsettledDailyAssessmentTargets(
                        assessmentDate.minusDays(DAILY_CATCH_UP_DAYS - 1L),
                        assessmentDate
                );
        Map<LocalDate, List<AccountDailySnapshotDto>> targetsByDate = unsettledTargets.stream()
                .collect(Collectors.groupingBy(
                        AccountDailySnapshotDto::getSnapshotDate,
                        TreeMap::new,
                        Collectors.toList()
                ));
        targetsByDate.forEach(this::calculateDailyAssessmentsSafely);
    }

    private void calculateDailyAssessmentsSafely(
            LocalDate assessmentDate,
            List<AccountDailySnapshotDto> targets) {
        int succeededCount = 0;
        int failedCount = 0;
        int assessmentCount = 0;
        for (AccountDailySnapshotDto target : targets) {
            try {
                assessmentCount += virtualInvestmentPeriodAssessmentService
                        .calculateDailyAssessment(target);
                succeededCount++;
            } catch (RuntimeException exception) {
                failedCount++;
                log.error(
                        "가상투자 일일 성향 재정산 실패: assessmentDate={}, accountId={}",
                        assessmentDate,
                        target.getAccountId(),
                        exception
                );
            }
        }
        log.info(
                "가상투자 일일 성향 재정산 완료: assessmentDate={}, targets={}, succeeded={}, failed={}, assessments={}",
                assessmentDate,
                targets.size(),
                succeededCount,
                failedCount,
                assessmentCount
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
