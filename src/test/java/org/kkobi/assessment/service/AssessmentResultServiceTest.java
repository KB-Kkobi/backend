package org.kkobi.assessment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.calculator.PersonaClassifier;
import org.kkobi.assessment.domain.AssessmentResultDetails;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.dto.AssessmentResultResponseDto;
import org.kkobi.assessment.mapper.AssessmentMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AssessmentResultServiceTest {

    @Test
    @DisplayName("저장된 진단 이력이 있으면 최신 결과를 반환한다.")
    void returnsLatestAssessmentResultWhenExists() {
        AssessmentResultResponseDto storedResult = new AssessmentResultResponseDto();
        storedResult.setResultId(1024L);
        storedResult.setUserId(1L);
        storedResult.setRtScore(new BigDecimal("50.00"));
        storedResult.setLhScore(new BigDecimal("50.00"));
        storedResult.setRpScore(new BigDecimal("50.00"));
        AssessmentResultService assessmentResultService = createAssessmentResultService(storedResult);

        AssessmentResultResponseDto result = assessmentResultService.getLatestAssessmentResult(1L);

        assertEquals(1024L, result.getResultId());
        assertEquals(1L, result.getUserId());
    }

    @Test
    @DisplayName("저장된 진단 이력이 없으면 예외를 발생시킨다.")
    void throwsExceptionWhenNoAssessmentResultExists() {
        AssessmentResultService assessmentResultService = createAssessmentResultService(null);

        assertThrows(
                IllegalArgumentException.class,
                () -> assessmentResultService.getLatestAssessmentResult(1L)
        );
    }

    private AssessmentResultService createAssessmentResultService(
            AssessmentResultResponseDto latestAssessmentResult) {
        AssessmentMapper assessmentMapper = new AssessmentMapper() {
            @Override
            public AssessmentScore getLatestAssessmentScore(Long userId) {
                return null;
            }

            @Override
            public AssessmentResultDetails getLatestAssessmentResultDetails(Long userId) {
                return null;
            }

            @Override
            public AssessmentResultResponseDto getLatestAssessmentResult(Long userId) {
                return latestAssessmentResult;
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
                return 1;
            }
        };
        return new AssessmentResultService(assessmentMapper, new PersonaClassifier());
    }
}