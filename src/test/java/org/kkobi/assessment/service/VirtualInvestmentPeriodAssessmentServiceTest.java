package org.kkobi.assessment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.calculator.AssetRatioCalculator;
import org.kkobi.assessment.calculator.BehaviorRuleEngine;
import org.kkobi.assessment.calculator.PersonaClassifier;
import org.kkobi.assessment.calculator.VirtualInvestmentPeriodCalculator;
import org.kkobi.assessment.calculator.VirtualInvestmentScoreCalculator;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.dto.AccountDailySnapshotDto;
import org.kkobi.assessment.enums.AssessmentPeriodType;
import org.kkobi.assessment.mapper.AccountDailySnapshotMapper;
import org.kkobi.assessment.mapper.AssessmentMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VirtualInvestmentPeriodAssessmentServiceTest {

    @Test
    @DisplayName("일일 정산에서 7일 자산 비중을 EMA에 반영한다.")
    void calculateDailyAssessments() {
        LocalDate firstTradeDate = LocalDate.of(2026, 8, 1);
        InMemoryAccountDailySnapshotMapper snapshotMapper =
                new InMemoryAccountDailySnapshotMapper(
                        firstTradeDate,
                        createSnapshots(firstTradeDate, 7, 10L, 80L, 10L),
                        1
                );
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        VirtualInvestmentPeriodAssessmentService assessmentService = createAssessmentService(
                snapshotMapper,
                assessmentMapper
        );

        int firstAssessmentCount = assessmentService.calculateDailyAssessments(
                firstTradeDate.plusDays(6)
        );
        int secondAssessmentCount = assessmentService.calculateDailyAssessments(
                firstTradeDate.plusDays(6)
        );

        assertEquals(1, firstAssessmentCount);
        assertEquals(0, secondAssessmentCount);
        assertEquals(1, assessmentMapper.getSavedResultCount());
        assertScoreEquals("53.33", assessmentMapper.getLatestScore().getRtScore());
        assertScoreEquals("48.34", assessmentMapper.getLatestScore().getLhScore());
        assertScoreEquals("51.67", assessmentMapper.getLatestScore().getRpScore());
    }

    @Test
    @DisplayName("첫 매매 후 7일째 서버가 중지되어도 다음 실행에서 자산 비중 규칙을 반영한다.")
    void calculateDelayedSevenDayAssessment() {
        LocalDate firstTradeDate = LocalDate.of(2026, 8, 1);
        InMemoryAccountDailySnapshotMapper snapshotMapper =
                new InMemoryAccountDailySnapshotMapper(
                        firstTradeDate,
                        createSnapshots(firstTradeDate, 8, 10L, 80L, 10L),
                        1
                );
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        VirtualInvestmentPeriodAssessmentService assessmentService = createAssessmentService(
                snapshotMapper,
                assessmentMapper
        );

        int assessmentCount = assessmentService.calculateDailyAssessments(
                firstTradeDate.plusDays(7)
        );

        assertEquals(1, assessmentCount);
        assertEquals(1, assessmentMapper.getSavedResultCount());
        assertScoreEquals("53.33", assessmentMapper.getLatestScore().getRtScore());
        assertScoreEquals("48.34", assessmentMapper.getLatestScore().getLhScore());
        assertScoreEquals("51.67", assessmentMapper.getLatestScore().getRpScore());
    }

    @Test
    @DisplayName("일평균 거래 횟수는 직전 금요일 기준으로 주간 EMA에 반영한다.")
    void calculateWeeklyTradeFrequencyAssessment() {
        LocalDate firstTradeDate = LocalDate.of(2026, 8, 3);
        InMemoryAccountDailySnapshotMapper snapshotMapper =
                new InMemoryAccountDailySnapshotMapper(
                        firstTradeDate,
                        List.of(),
                        1
                );
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        VirtualInvestmentPeriodAssessmentService assessmentService = createAssessmentService(
                snapshotMapper,
                assessmentMapper
        );

        int firstAssessmentCount = assessmentService.calculateWeeklyTradeFrequencyAssessments(
                LocalDate.of(2026, 8, 9)
        );
        int secondAssessmentCount = assessmentService.calculateWeeklyTradeFrequencyAssessments(
                LocalDate.of(2026, 8, 9)
        );

        assertEquals(1, firstAssessmentCount);
        assertEquals(0, secondAssessmentCount);
        assertEquals(1, assessmentMapper.getSavedResultCount());
        assertScoreEquals("50.00", assessmentMapper.getLatestScore().getRtScore());
        assertScoreEquals("50.00", assessmentMapper.getLatestScore().getLhScore());
        assertScoreEquals("48.34", assessmentMapper.getLatestScore().getRpScore());
    }

    @Test
    @DisplayName("월요일부터 금요일까지 같은 현금 비중 구간을 유지하면 주간 EMA에 반영한다.")
    void calculateWeeklyCashAssessment() {
        LocalDate periodStartDate = LocalDate.of(2026, 8, 3);
        InMemoryAccountDailySnapshotMapper snapshotMapper =
                new InMemoryAccountDailySnapshotMapper(
                        LocalDate.of(2026, 7, 1),
                        createSnapshots(periodStartDate, 5, 60L, 40L, 0L),
                        1
                );
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        VirtualInvestmentPeriodAssessmentService assessmentService = createAssessmentService(
                snapshotMapper,
                assessmentMapper
        );

        int firstAssessmentCount = assessmentService.calculateWeeklyCashAssessments(
                LocalDate.of(2026, 8, 9)
        );
        int secondAssessmentCount = assessmentService.calculateWeeklyCashAssessments(
                LocalDate.of(2026, 8, 9)
        );

        assertEquals(1, firstAssessmentCount);
        assertEquals(0, secondAssessmentCount);
        assertEquals(1, assessmentMapper.getSavedResultCount());
        assertScoreEquals("46.67", assessmentMapper.getLatestScore().getRtScore());
        assertScoreEquals("55.00", assessmentMapper.getLatestScore().getLhScore());
        assertScoreEquals("48.34", assessmentMapper.getLatestScore().getRpScore());
    }

    @Test
    @DisplayName("매도하지 않은 증권의 평균 보유 기간이 30일 이상이면 EMA에 한 번 반영한다.")
    void calculateLongSecurityHoldingOnce() {
        LocalDate firstTradeDate = LocalDate.of(2026, 8, 1);
        InMemoryAccountDailySnapshotMapper snapshotMapper =
                new InMemoryAccountDailySnapshotMapper(
                        firstTradeDate,
                        List.of(),
                        32
                );
        snapshotMapper.setSecurityHoldingPeriod(
                firstTradeDate,
                BigDecimal.valueOf(31)
        );
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        VirtualInvestmentPeriodAssessmentService assessmentService = createAssessmentService(
                snapshotMapper,
                assessmentMapper
        );

        int firstAssessmentCount = assessmentService.calculateDailyAssessments(
                LocalDate.of(2026, 9, 1)
        );
        int secondAssessmentCount = assessmentService.calculateDailyAssessments(
                LocalDate.of(2026, 9, 2)
        );

        assertEquals(1, firstAssessmentCount);
        assertEquals(0, secondAssessmentCount);
        assertEquals(1, assessmentMapper.getSavedResultCount());
        assertScoreEquals("50.00", assessmentMapper.getLatestScore().getRtScore());
        assertScoreEquals("48.34", assessmentMapper.getLatestScore().getLhScore());
        assertScoreEquals("48.34", assessmentMapper.getLatestScore().getRpScore());
    }

    private VirtualInvestmentPeriodAssessmentService createAssessmentService(
            AccountDailySnapshotMapper snapshotMapper,
            AssessmentMapper assessmentMapper) {
        return new VirtualInvestmentPeriodAssessmentService(
                snapshotMapper,
                new VirtualInvestmentPeriodCalculator(new AssetRatioCalculator()),
                new BehaviorRuleEngine(),
                new InMemoryAssessmentSettlementService(),
                new VirtualInvestmentPeriodResultService(
                        new VirtualInvestmentScoreCalculator(),
                        new AssessmentResultService(assessmentMapper, new PersonaClassifier())
                )
        );
    }

    private List<AccountDailySnapshotDto> createSnapshots(
            LocalDate startDate,
            int count,
            long currentCash,
            long currentStockPrincipal,
            long currentDeposit) {
        return IntStream.range(0, count)
                .mapToObj(dayOffset -> createSnapshot(
                        startDate.plusDays(dayOffset),
                        currentCash,
                        currentStockPrincipal,
                        currentDeposit
                ))
                .toList();
    }

    private AccountDailySnapshotDto createSnapshot(
            LocalDate snapshotDate,
            long currentCash,
            long currentStockPrincipal,
            long currentDeposit) {
        AccountDailySnapshotDto snapshot = new AccountDailySnapshotDto();
        snapshot.setAccountId(1L);
        snapshot.setUserId(1L);
        snapshot.setSnapshotDate(snapshotDate);
        snapshot.setCurrentCash(currentCash);
        snapshot.setCurrentStockPrincipal(currentStockPrincipal);
        snapshot.setCurrentDeposit(currentDeposit);
        snapshot.setTotalInvestedPrincipal(
                currentCash + currentStockPrincipal + currentDeposit
        );
        snapshot.setCashRatio(BigDecimal.valueOf(currentCash));
        return snapshot;
    }

    private void assertScoreEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private static class InMemoryAccountDailySnapshotMapper
            implements AccountDailySnapshotMapper {

        private final LocalDate firstTradeDate;
        private final List<AccountDailySnapshotDto> snapshots;
        private final int completedTradeCount;
        private LocalDate securityHoldingStartedDate;
        private BigDecimal averageSecurityHoldingDays;

        private InMemoryAccountDailySnapshotMapper(
                LocalDate firstTradeDate,
                List<AccountDailySnapshotDto> snapshots,
                int completedTradeCount) {
            this.firstTradeDate = firstTradeDate;
            this.snapshots = snapshots;
            this.completedTradeCount = completedTradeCount;
        }

        @Override
        public List<AccountDailySnapshotDto> getAccountSnapshotTargets(LocalDate snapshotDate) {
            AccountDailySnapshotDto account = new AccountDailySnapshotDto();
            account.setAccountId(1L);
            account.setUserId(1L);
            account.setVirtualInvestmentStartedDate(firstTradeDate.minusDays(1));
            account.setSecurityHoldingStartedDate(securityHoldingStartedDate);
            account.setAverageSecurityHoldingDays(averageSecurityHoldingDays);
            return List.of(account);
        }

        @Override
        public int saveAccountDailySnapshot(AccountDailySnapshotDto accountDailySnapshot) {
            return 1;
        }

        @Override
        public List<AccountDailySnapshotDto> getAccountDailySnapshots(
                Long accountId,
                LocalDate startDate,
                LocalDate endDate) {
            return snapshots.stream()
                    .filter(snapshot -> !snapshot.getSnapshotDate().isBefore(startDate))
                    .filter(snapshot -> !snapshot.getSnapshotDate().isAfter(endDate))
                    .toList();
        }

        @Override
        public LocalDate getFirstCompletedSecurityOrderDate(Long accountId) {
            return firstTradeDate;
        }

        @Override
        public int getCompletedSecurityOrderCount(Long accountId, LocalDate assessmentDate) {
            return completedTradeCount;
        }

        private void setSecurityHoldingPeriod(
                LocalDate securityHoldingStartedDate,
                BigDecimal averageSecurityHoldingDays) {
            this.securityHoldingStartedDate = securityHoldingStartedDate;
            this.averageSecurityHoldingDays = averageSecurityHoldingDays;
        }
    }

    private static class InMemoryAssessmentSettlementService
            extends AssessmentSettlementService {

        private final Set<String> settlementKeys = new HashSet<>();

        private InMemoryAssessmentSettlementService() {
            super(null);
        }

        @Override
        public boolean canStartAssessment(
                Long accountId,
                AssessmentPeriodType assessmentPeriodType,
                LocalDate periodDate) {
            return settlementKeys.add(createSettlementKey(
                    accountId,
                    assessmentPeriodType,
                    periodDate
            ));
        }

        @Override
        public void completeAssessment(
                Long accountId,
                AssessmentPeriodType assessmentPeriodType,
                LocalDate periodDate) {
        }

        @Override
        public void cancelAssessment(
                Long accountId,
                AssessmentPeriodType assessmentPeriodType,
                LocalDate periodDate) {
            settlementKeys.remove(createSettlementKey(
                    accountId,
                    assessmentPeriodType,
                    periodDate
            ));
        }

        private String createSettlementKey(
                Long accountId,
                AssessmentPeriodType assessmentPeriodType,
                LocalDate periodDate) {
            return accountId + ":" + assessmentPeriodType + ":" + periodDate;
        }
    }

    private static class InMemoryAssessmentMapper implements AssessmentMapper {

        private AssessmentScore latestScore;
        private int savedResultCount;

        @Override
        public AssessmentScore getLatestAssessmentScore(Long userId) {
            return latestScore;
        }

        @Override
        public org.kkobi.assessment.domain.AssessmentResultDetails
                getLatestAssessmentResultDetails(Long userId) {
            return null;
        }

        @Override
        public Long getPersonaIdByAxisCode(String axisCode) {
            return 1L;
        }

        @Override
        public int saveAssessmentResult(
                Long userId,
                Long personaId,
                AssessmentScore assessmentScore) {
            latestScore = assessmentScore;
            savedResultCount++;
            return 1;
        }

        private AssessmentScore getLatestScore() {
            return latestScore;
        }

        private int getSavedResultCount() {
            return savedResultCount;
        }
    }
}
