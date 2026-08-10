package org.kkobi.assessment.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.enums.AssessmentPeriodType;
import org.kkobi.assessment.mapper.AssessmentSettlementMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AssessmentSettlementService {

    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(10);

    private final AssessmentSettlementMapper assessmentSettlementMapper;

    public boolean canStartAssessment(
            Long accountId,
            AssessmentPeriodType assessmentPeriodType,
            LocalDate periodDate) {
        try {
            assessmentSettlementMapper.saveProcessingAssessmentSettlement(
                    accountId,
                    assessmentPeriodType,
                    periodDate
            );
            return true;
        } catch (DuplicateKeyException exception) {
            return assessmentSettlementMapper.restartStaleAssessmentSettlement(
                    accountId,
                    assessmentPeriodType,
                    periodDate,
                    LocalDateTime.now().minus(PROCESSING_TIMEOUT)
            ) == 1;
        }
    }

    public void completeAssessment(
            Long accountId,
            AssessmentPeriodType assessmentPeriodType,
            LocalDate periodDate) {
        int updatedCount = assessmentSettlementMapper.completeAssessmentSettlement(
                accountId,
                assessmentPeriodType,
                periodDate
        );
        if (updatedCount != 1) {
            throw new IllegalStateException("진행 중인 성향 정산을 찾을 수 없습니다.");
        }
    }

    public void cancelAssessment(
            Long accountId,
            AssessmentPeriodType assessmentPeriodType,
            LocalDate periodDate) {
        assessmentSettlementMapper.deleteProcessingAssessmentSettlement(
                accountId,
                assessmentPeriodType,
                periodDate
        );
    }
}
