package org.kkobi.assessment.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.assessment.enums.AssessmentPeriodType;

import java.time.LocalDate;
import java.time.LocalDateTime;

public interface AssessmentSettlementMapper {

    int saveProcessingAssessmentSettlement(
            @Param("accountId") Long accountId,
            @Param("assessmentPeriodType") AssessmentPeriodType assessmentPeriodType,
            @Param("periodDate") LocalDate periodDate
    );

    int restartStaleAssessmentSettlement(
            @Param("accountId") Long accountId,
            @Param("assessmentPeriodType") AssessmentPeriodType assessmentPeriodType,
            @Param("periodDate") LocalDate periodDate,
            @Param("staleBefore") LocalDateTime staleBefore
    );

    int completeAssessmentSettlement(
            @Param("accountId") Long accountId,
            @Param("assessmentPeriodType") AssessmentPeriodType assessmentPeriodType,
            @Param("periodDate") LocalDate periodDate
    );

    int deleteProcessingAssessmentSettlement(
            @Param("accountId") Long accountId,
            @Param("assessmentPeriodType") AssessmentPeriodType assessmentPeriodType,
            @Param("periodDate") LocalDate periodDate
    );
}
