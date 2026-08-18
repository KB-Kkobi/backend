package org.kkobi.assessment.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.calculator.BehaviorRuleEngine;
import org.kkobi.assessment.calculator.VirtualInvestmentPeriodCalculator;
import org.kkobi.assessment.calculator.VirtualInvestmentFollowUpCalculator;
import org.kkobi.assessment.domain.BehaviorAnalysisResult;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.dto.AccountDailySnapshotDto;
import org.kkobi.assessment.enums.AssessmentPeriodType;
import org.kkobi.assessment.mapper.AccountDailySnapshotMapper;
import org.kkobi.assessment.mapper.VirtualInvestmentBehaviorMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.sql.Timestamp;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VirtualInvestmentPeriodAssessmentService {

    private static final int SEVEN_DAY_PERIOD = 7;
    private static final int CASH_MAINTENANCE_DAYS = 5;
    private static final int FOLLOW_UP_LOOKBACK_DAYS = 10;

    private final AccountDailySnapshotMapper accountDailySnapshotMapper;
    private final VirtualInvestmentBehaviorMapper virtualInvestmentBehaviorMapper;
    private final VirtualInvestmentPeriodCalculator virtualInvestmentPeriodCalculator;
    private final VirtualInvestmentFollowUpCalculator virtualInvestmentFollowUpCalculator;
    private final BehaviorRuleEngine behaviorRuleEngine;
    private final AssessmentSettlementService assessmentSettlementService;
    private final VirtualInvestmentPeriodResultService virtualInvestmentPeriodResultService;

    public int calculateDailyAssessments(LocalDate assessmentDate) {
        return accountDailySnapshotMapper.getAccountSnapshotTargets(assessmentDate)
                .stream()
                .mapToInt(account -> calculateDailyAccountAssessments(account, assessmentDate))
                .sum();
    }

    private int calculateDailyAccountAssessments(
            AccountDailySnapshotDto account,
            LocalDate assessmentDate) {
        if (!assessmentSettlementService.canStartAssessment(
                account.getAccountId(),
                AssessmentPeriodType.DAILY_ASSESSMENT_BATCH,
                assessmentDate
        )) {
            return 0;
        }

        int assessmentCount;
        try {
            assessmentCount = calculateSevenDayAllocation(account, assessmentDate)
                    + calculateLongSecurityHolding(account)
                    + calculateFollowUpBehaviors(account, assessmentDate);
        } catch (RuntimeException exception) {
            assessmentSettlementService.cancelAssessment(
                    account.getAccountId(),
                    AssessmentPeriodType.DAILY_ASSESSMENT_BATCH,
                    assessmentDate
            );
            throw exception;
        }
        assessmentSettlementService.completeAssessment(
                account.getAccountId(),
                AssessmentPeriodType.DAILY_ASSESSMENT_BATCH,
                assessmentDate
        );
        return assessmentCount;
    }

    public int calculateWeeklyCashAssessments(LocalDate assessmentDate) {
        LocalDate periodEndDate = calculateWeeklyPeriodEndDate(assessmentDate);
        LocalDate periodStartDate = periodEndDate.minusDays(CASH_MAINTENANCE_DAYS - 1L);

        return accountDailySnapshotMapper.getAccountSnapshotTargets(periodEndDate)
                .stream()
                .mapToInt(account -> calculateMaintainedCashRatio(
                        account,
                        periodStartDate,
                        periodEndDate
                ))
                .sum();
    }

    public int calculateWeeklyTradeFrequencyAssessments(LocalDate assessmentDate) {
        LocalDate periodEndDate = calculateWeeklyPeriodEndDate(assessmentDate);
        return accountDailySnapshotMapper.getAccountSnapshotTargets(periodEndDate)
                .stream()
                .mapToInt(account -> calculateTradeFrequency(account, periodEndDate))
                .sum();
    }

    public int calculateWeeklyBalanceAssessments(LocalDate assessmentDate) {
        LocalDate periodEndDate = calculateWeeklyPeriodEndDate(assessmentDate);
        LocalDate periodStartDate = periodEndDate.minusDays(CASH_MAINTENANCE_DAYS - 1L);
        return accountDailySnapshotMapper.getAccountSnapshotTargets(periodEndDate)
                .stream()
                .mapToInt(account -> calculateMaintainedBalance(
                        account,
                        periodStartDate,
                        periodEndDate
                ))
                .sum();
    }

    private int calculateFollowUpBehaviors(
            AccountDailySnapshotDto account,
            LocalDate assessmentDate) {
        List<AccountDailySnapshotDto> snapshots = accountDailySnapshotMapper
                .getAccountDailySnapshots(
                        account.getAccountId(),
                        assessmentDate.minusDays(FOLLOW_UP_LOOKBACK_DAYS),
                        assessmentDate
                );
        BehaviorContext context = virtualInvestmentFollowUpCalculator.calculateFollowUpContext(
                virtualInvestmentBehaviorMapper.getPreviousVirtualInvestmentBehaviors(
                        account.getAccountId(),
                        Timestamp.valueOf(assessmentDate.plusDays(1).atStartOfDay())
                ),
                snapshots,
                assessmentDate
        );
        return savePeriodAssessment(
                account,
                AssessmentPeriodType.FOLLOW_UP_BEHAVIOR,
                assessmentDate,
                behaviorRuleEngine.calculateVirtualInvestmentPeriodAnalysis(List.of(context))
        );
    }

    private int calculateMaintainedBalance(
            AccountDailySnapshotDto account,
            LocalDate periodStartDate,
            LocalDate periodEndDate) {
        List<AccountDailySnapshotDto> snapshots = accountDailySnapshotMapper
                .getAccountDailySnapshots(account.getAccountId(), periodStartDate, periodEndDate);
        if (!hasConsecutiveDates(snapshots, periodStartDate, CASH_MAINTENANCE_DAYS)) {
            return 0;
        }

        BehaviorContext context = new BehaviorContext();
        context.setRiskBudgetMaintenance(
                accountDailySnapshotMapper.getCompletedSecurityOrderCountBetween(
                        account.getAccountId(),
                        periodStartDate,
                        periodEndDate
                ) == 0 && virtualInvestmentPeriodCalculator.isRiskBudgetMaintained(snapshots)
        );
        return savePeriodAssessment(
                account,
                AssessmentPeriodType.WEEKLY_BALANCE_MAINTENANCE,
                periodEndDate,
                behaviorRuleEngine.calculateVirtualInvestmentPeriodAnalysis(List.of(context))
        );
    }

    private int calculateSevenDayAllocation(
            AccountDailySnapshotDto account,
            LocalDate assessmentDate) {
        LocalDate firstTradeDate = accountDailySnapshotMapper
                .getFirstCompletedSecurityOrderDate(account.getAccountId());
        if (firstTradeDate == null) {
            return 0;
        }

        LocalDate periodEndDate = firstTradeDate.plusDays(SEVEN_DAY_PERIOD - 1L);
        if (assessmentDate.isBefore(periodEndDate)) {
            return 0;
        }

        List<AccountDailySnapshotDto> snapshots = accountDailySnapshotMapper
                .getAccountDailySnapshots(
                        account.getAccountId(),
                        firstTradeDate,
                        periodEndDate
                );
        if (!hasConsecutiveDates(snapshots, firstTradeDate, SEVEN_DAY_PERIOD)) {
            return 0;
        }

        BehaviorContext context = new BehaviorContext();
        context.setSevenDayAverageStockRatio(
                virtualInvestmentPeriodCalculator.calculateAverageStockRatio(snapshots)
        );
        context.setSevenDayAverageCashRatio(
                virtualInvestmentPeriodCalculator.calculateAverageCashRatio(snapshots)
        );
        return savePeriodAssessment(
                account,
                AssessmentPeriodType.SEVEN_DAY_ALLOCATION,
                periodEndDate,
                behaviorRuleEngine.calculateVirtualInvestmentPeriodAnalysis(List.of(context))
        );
    }

    private int calculateTradeFrequency(
            AccountDailySnapshotDto account,
            LocalDate assessmentDate) {
        LocalDate virtualInvestmentStartedDate = account.getVirtualInvestmentStartedDate();
        if (virtualInvestmentStartedDate == null
                || assessmentDate.isBefore(virtualInvestmentStartedDate)) {
            return 0;
        }

        int completedTradeCount = accountDailySnapshotMapper.getCompletedSecurityOrderCount(
                account.getAccountId(),
                assessmentDate
        );
        BehaviorContext context = new BehaviorContext();
        context.setAverageDailyTradeCount(
                virtualInvestmentPeriodCalculator.calculateAverageDailyTradeCount(
                        completedTradeCount,
                        virtualInvestmentStartedDate,
                        assessmentDate
                )
        );
        return savePeriodAssessment(
                account,
                AssessmentPeriodType.WEEKLY_TRADE_FREQUENCY,
                assessmentDate,
                behaviorRuleEngine.calculateVirtualInvestmentPeriodAnalysis(List.of(context))
        );
    }

    private int calculateLongSecurityHolding(AccountDailySnapshotDto account) {
        if (account.getSecurityHoldingStartedDate() == null
                || account.getAverageSecurityHoldingDays() == null) {
            return 0;
        }

        BehaviorContext context = new BehaviorContext();
        context.setAverageHoldingDays(account.getAverageSecurityHoldingDays());
        return savePeriodAssessment(
                account,
                AssessmentPeriodType.LONG_SECURITY_HOLDING,
                account.getSecurityHoldingStartedDate(),
                behaviorRuleEngine.calculateVirtualInvestmentPeriodAnalysis(List.of(context))
        );
    }

    private int calculateMaintainedCashRatio(
            AccountDailySnapshotDto account,
            LocalDate periodStartDate,
            LocalDate periodEndDate) {
        LocalDate virtualInvestmentStartedDate = account.getVirtualInvestmentStartedDate();
        if (virtualInvestmentStartedDate == null
                || virtualInvestmentStartedDate.isAfter(periodStartDate)) {
            return 0;
        }

        List<AccountDailySnapshotDto> snapshots = accountDailySnapshotMapper
                .getAccountDailySnapshots(
                        account.getAccountId(),
                        periodStartDate,
                        periodEndDate
                );
        if (!hasConsecutiveDates(snapshots, periodStartDate, CASH_MAINTENANCE_DAYS)) {
            return 0;
        }

        BigDecimal maintainedCashRatio = virtualInvestmentPeriodCalculator
                .calculateMaintainedCashRatio(snapshots);
        BehaviorContext context = new BehaviorContext();
        context.setCashRatio(maintainedCashRatio);
        context.setMaintainedCashRatioDays(CASH_MAINTENANCE_DAYS);
        return savePeriodAssessment(
                account,
                AssessmentPeriodType.WEEKLY_CASH_RATIO,
                periodEndDate,
                behaviorRuleEngine.calculateVirtualInvestmentPeriodAnalysis(List.of(context))
        );
    }

    private int savePeriodAssessment(
            AccountDailySnapshotDto account,
            AssessmentPeriodType assessmentPeriodType,
            LocalDate periodDate,
            BehaviorAnalysisResult analysisResult) {
        if (!analysisResult.existsAppliedRule()) {
            return 0;
        }

        if (!assessmentSettlementService.canStartAssessment(
                account.getAccountId(),
                assessmentPeriodType,
                periodDate
        )) {
            return 0;
        }

        try {
            virtualInvestmentPeriodResultService.saveVirtualInvestmentPeriodResult(
                    account.getUserId(),
                    analysisResult
            );
        } catch (RuntimeException exception) {
            assessmentSettlementService.cancelAssessment(
                    account.getAccountId(),
                    assessmentPeriodType,
                    periodDate
            );
            throw exception;
        }
        assessmentSettlementService.completeAssessment(
                account.getAccountId(),
                assessmentPeriodType,
                periodDate
        );
        return 1;
    }

    private boolean hasConsecutiveDates(
            List<AccountDailySnapshotDto> snapshots,
            LocalDate startDate,
            int expectedDays) {
        if (snapshots.size() != expectedDays) {
            return false;
        }
        for (int dayOffset = 0; dayOffset < expectedDays; dayOffset++) {
            if (!startDate.plusDays(dayOffset).equals(snapshots.get(dayOffset).getSnapshotDate())) {
                return false;
            }
        }
        return true;
    }

    private LocalDate calculateWeeklyPeriodEndDate(LocalDate assessmentDate) {
        return assessmentDate.with(
                TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY)
        );
    }
}
