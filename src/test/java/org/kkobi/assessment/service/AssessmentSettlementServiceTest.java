package org.kkobi.assessment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.enums.AssessmentPeriodType;
import org.kkobi.assessment.mapper.AssessmentSettlementMapper;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentSettlementServiceTest {

    private static final Long ACCOUNT_ID = 1L;
    private static final LocalDate PERIOD_DATE = LocalDate.of(2026, 8, 7);
    private static final AssessmentPeriodType PERIOD_TYPE =
            AssessmentPeriodType.WEEKLY_CASH_RATIO;

    @Test
    @DisplayName("동일 계좌와 정산 기준으로 진행 중인 정산을 중복 시작하지 않는다.")
    void preventDuplicateProcessingAssessment() {
        InMemoryAssessmentSettlementMapper mapper =
                new InMemoryAssessmentSettlementMapper();
        AssessmentSettlementService service = new AssessmentSettlementService(mapper);

        assertTrue(service.canStartAssessment(ACCOUNT_ID, PERIOD_TYPE, PERIOD_DATE));
        assertFalse(service.canStartAssessment(ACCOUNT_ID, PERIOD_TYPE, PERIOD_DATE));
    }

    @Test
    @DisplayName("완료된 정산은 다시 시작하지 않는다.")
    void preventCompletedAssessmentRetry() {
        InMemoryAssessmentSettlementMapper mapper =
                new InMemoryAssessmentSettlementMapper();
        AssessmentSettlementService service = new AssessmentSettlementService(mapper);

        assertTrue(service.canStartAssessment(ACCOUNT_ID, PERIOD_TYPE, PERIOD_DATE));
        service.completeAssessment(ACCOUNT_ID, PERIOD_TYPE, PERIOD_DATE);

        assertTrue(mapper.isCompleted(ACCOUNT_ID, PERIOD_TYPE, PERIOD_DATE));
        assertFalse(service.canStartAssessment(ACCOUNT_ID, PERIOD_TYPE, PERIOD_DATE));
    }

    @Test
    @DisplayName("실패한 정산의 진행 상태를 취소하면 다시 실행할 수 있다.")
    void retryCancelledAssessment() {
        InMemoryAssessmentSettlementMapper mapper =
                new InMemoryAssessmentSettlementMapper();
        AssessmentSettlementService service = new AssessmentSettlementService(mapper);

        assertTrue(service.canStartAssessment(ACCOUNT_ID, PERIOD_TYPE, PERIOD_DATE));
        service.cancelAssessment(ACCOUNT_ID, PERIOD_TYPE, PERIOD_DATE);

        assertNull(mapper.getSettlement(ACCOUNT_ID, PERIOD_TYPE, PERIOD_DATE));
        assertTrue(service.canStartAssessment(ACCOUNT_ID, PERIOD_TYPE, PERIOD_DATE));
    }

    @Test
    @DisplayName("10분 이상 멈춘 진행 중 정산은 다시 실행할 수 있다.")
    void restartStaleProcessingAssessment() {
        InMemoryAssessmentSettlementMapper mapper =
                new InMemoryAssessmentSettlementMapper();
        AssessmentSettlementService service = new AssessmentSettlementService(mapper);

        assertTrue(service.canStartAssessment(ACCOUNT_ID, PERIOD_TYPE, PERIOD_DATE));
        mapper.makeSettlementStale(ACCOUNT_ID, PERIOD_TYPE, PERIOD_DATE);

        assertTrue(service.canStartAssessment(ACCOUNT_ID, PERIOD_TYPE, PERIOD_DATE));
        assertFalse(service.canStartAssessment(ACCOUNT_ID, PERIOD_TYPE, PERIOD_DATE));
    }

    private static class InMemoryAssessmentSettlementMapper
            implements AssessmentSettlementMapper {

        private final Map<SettlementKey, SettlementRecord> settlements = new HashMap<>();

        @Override
        public int saveProcessingAssessmentSettlement(
                Long accountId,
                AssessmentPeriodType assessmentPeriodType,
                LocalDate periodDate) {
            SettlementKey settlementKey = new SettlementKey(
                    accountId,
                    assessmentPeriodType,
                    periodDate
            );
            if (settlements.containsKey(settlementKey)) {
                throw new DuplicateKeyException("중복 정산");
            }
            settlements.put(
                    settlementKey,
                    new SettlementRecord("PROCESSING", LocalDateTime.now())
            );
            return 1;
        }

        @Override
        public int restartStaleAssessmentSettlement(
                Long accountId,
                AssessmentPeriodType assessmentPeriodType,
                LocalDate periodDate,
                LocalDateTime staleBefore) {
            SettlementRecord settlement = getSettlement(
                    accountId,
                    assessmentPeriodType,
                    periodDate
            );
            if (settlement == null
                    || !"PROCESSING".equals(settlement.status)
                    || !settlement.updatedAt.isBefore(staleBefore)) {
                return 0;
            }
            settlement.updatedAt = LocalDateTime.now();
            return 1;
        }

        @Override
        public int completeAssessmentSettlement(
                Long accountId,
                AssessmentPeriodType assessmentPeriodType,
                LocalDate periodDate) {
            SettlementRecord settlement = getSettlement(
                    accountId,
                    assessmentPeriodType,
                    periodDate
            );
            if (settlement == null || !"PROCESSING".equals(settlement.status)) {
                return 0;
            }
            settlement.status = "COMPLETED";
            settlement.updatedAt = LocalDateTime.now();
            return 1;
        }

        @Override
        public int deleteProcessingAssessmentSettlement(
                Long accountId,
                AssessmentPeriodType assessmentPeriodType,
                LocalDate periodDate) {
            SettlementKey settlementKey = new SettlementKey(
                    accountId,
                    assessmentPeriodType,
                    periodDate
            );
            SettlementRecord settlement = settlements.get(settlementKey);
            if (settlement == null || !"PROCESSING".equals(settlement.status)) {
                return 0;
            }
            settlements.remove(settlementKey);
            return 1;
        }

        private SettlementRecord getSettlement(
                Long accountId,
                AssessmentPeriodType assessmentPeriodType,
                LocalDate periodDate) {
            return settlements.get(new SettlementKey(
                    accountId,
                    assessmentPeriodType,
                    periodDate
            ));
        }

        private boolean isCompleted(
                Long accountId,
                AssessmentPeriodType assessmentPeriodType,
                LocalDate periodDate) {
            SettlementRecord settlement = getSettlement(
                    accountId,
                    assessmentPeriodType,
                    periodDate
            );
            return settlement != null && "COMPLETED".equals(settlement.status);
        }

        private void makeSettlementStale(
                Long accountId,
                AssessmentPeriodType assessmentPeriodType,
                LocalDate periodDate) {
            getSettlement(accountId, assessmentPeriodType, periodDate).updatedAt =
                    LocalDateTime.now().minusMinutes(11);
        }
    }

    private record SettlementKey(
            Long accountId,
            AssessmentPeriodType assessmentPeriodType,
            LocalDate periodDate) {
    }

    private static class SettlementRecord {

        private String status;
        private LocalDateTime updatedAt;

        private SettlementRecord(String status, LocalDateTime updatedAt) {
            this.status = status;
            this.updatedAt = updatedAt;
        }
    }
}
