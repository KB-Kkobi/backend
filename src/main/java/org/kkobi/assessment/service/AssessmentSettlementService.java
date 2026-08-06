package org.kkobi.assessment.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.enums.AssessmentPeriodType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class AssessmentSettlementService {

    private static final String KEY_PREFIX = "assessment:settlement:";
    private static final String PROCESSING = "PROCESSING";
    private static final String COMPLETED = "COMPLETED";
    private static final Duration PROCESSING_TIMEOUT = Duration.ofMinutes(10);
    private static final Duration COMPLETED_RETENTION = Duration.ofDays(400);

    private final StringRedisTemplate stringRedisTemplate;

    public boolean canStartAssessment(
            Long accountId,
            AssessmentPeriodType assessmentPeriodType,
            LocalDate periodDate) {
        Boolean started = stringRedisTemplate.opsForValue().setIfAbsent(
                createSettlementKey(accountId, assessmentPeriodType, periodDate),
                PROCESSING,
                PROCESSING_TIMEOUT
        );
        return Boolean.TRUE.equals(started);
    }

    public void completeAssessment(
            Long accountId,
            AssessmentPeriodType assessmentPeriodType,
            LocalDate periodDate) {
        String settlementKey = createSettlementKey(accountId, assessmentPeriodType, periodDate);
        if (assessmentPeriodType.isPermanent()) {
            stringRedisTemplate.opsForValue().set(settlementKey, COMPLETED);
            return;
        }
        stringRedisTemplate.opsForValue().set(
                settlementKey,
                COMPLETED,
                COMPLETED_RETENTION
        );
    }

    public void cancelAssessment(
            Long accountId,
            AssessmentPeriodType assessmentPeriodType,
            LocalDate periodDate) {
        stringRedisTemplate.delete(
                createSettlementKey(accountId, assessmentPeriodType, periodDate)
        );
    }

    private String createSettlementKey(
            Long accountId,
            AssessmentPeriodType assessmentPeriodType,
            LocalDate periodDate) {
        return KEY_PREFIX
                + assessmentPeriodType.name()
                + ":" + accountId
                + ":" + periodDate;
    }
}
