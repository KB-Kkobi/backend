package org.kkobi.assessment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.calculator.PersonaClassifier;
import org.kkobi.assessment.domain.AssessmentResult;
import org.kkobi.assessment.domain.AssessmentResultDetails;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.dto.AssessmentResultResponseDto;
import org.kkobi.assessment.mapper.AssessmentMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    @DisplayName("저장된 진단 이력이 없으면 null을 반환한다.")
    void returnsNullWhenNoAssessmentResultExists() {
        AssessmentResultService assessmentResultService = createAssessmentResultService(null);

        AssessmentResultResponseDto result = assessmentResultService.getLatestAssessmentResult(1L);

        assertNull(result);
    }

    @Test
    @DisplayName("저장된 진단 이력이 있으면 최신 상세 결과를 반환한다.")
    void returnsLatestAssessmentResultDetailsWhenExists() {
        AssessmentResultDetails storedDetails = new AssessmentResultDetails();
        storedDetails.setResultId(42L);
        storedDetails.setAxisCode("AA");
        AssessmentResultService assessmentResultService =
                createAssessmentResultServiceWithDetails(storedDetails);

        AssessmentResultDetails result =
                assessmentResultService.getLatestAssessmentResultDetails(1L);

        assertNotNull(result);
        assertEquals(42L, result.getResultId());
    }

    @Test
    @DisplayName("저장된 진단 이력이 없으면 null을 반환한다.")
    void returnsNullWhenNoAssessmentResultDetails() {
        AssessmentResultService assessmentResultService =
                createAssessmentResultServiceWithDetails(null);

        AssessmentResultDetails result =
                assessmentResultService.getLatestAssessmentResultDetails(1L);

        assertNull(result);
    }

    private AssessmentResultService createAssessmentResultServiceWithDetails(
            AssessmentResultDetails details) {
        AssessmentMapper assessmentMapper = new AssessmentMapper() {
            @Override
            public AssessmentScore getLatestAssessmentScore(Long userId) {
                return null;
            }

            @Override
            public AssessmentResultDetails getLatestAssessmentResultDetails(Long userId) {
                return details;
            }

            @Override
            public AssessmentResultResponseDto getLatestAssessmentResult(Long userId) {
                return null;
            }

            @Override
            public Long getPersonaIdByAxisCode(String axisCode) {
                return 1L;
            }

            @Override
            public int saveAssessmentResult(Long userId, Long personaId,
                    AssessmentScore assessmentScore) {
                return 1;
            }
        };
        return new AssessmentResultService(assessmentMapper, new PersonaClassifier());
    }

    @Test
    @DisplayName("저장한 RT, LH, RP 점수는 최신 성향 결과 조회 응답과 일치한다.")
    void returnsSameScoresAfterSavingAssessmentResult() {
        InMemoryAssessmentMapper assessmentMapper = new InMemoryAssessmentMapper();
        AssessmentResultService assessmentResultService = new AssessmentResultService(
                assessmentMapper,
                new PersonaClassifier()
        );
        AssessmentScore assessmentScore = new AssessmentScore(
                new BigDecimal("68.25"),
                new BigDecimal("55.10"),
                new BigDecimal("42.30")
        );

        AssessmentResult savedResult = assessmentResultService.saveAssessmentResult(
                1L,
                assessmentScore,
                List.of()
        );
        AssessmentResultResponseDto queriedResult = assessmentResultService
                .getLatestAssessmentResult(1L);

        assertScoreEquals(
                savedResult.getAssessmentScore().getRtScore(),
                queriedResult.getRtScore()
        );
        assertScoreEquals(
                savedResult.getAssessmentScore().getLhScore(),
                queriedResult.getLhScore()
        );
        assertScoreEquals(
                savedResult.getAssessmentScore().getRpScore(),
                queriedResult.getRpScore()
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

    private void assertScoreEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }

    private static class InMemoryAssessmentMapper implements AssessmentMapper {

        private Long savedUserId;
        private AssessmentScore savedAssessmentScore;

        @Override
        public AssessmentScore getLatestAssessmentScore(Long userId) {
            return savedAssessmentScore;
        }

        @Override
        public AssessmentResultDetails getLatestAssessmentResultDetails(Long userId) {
            return null;
        }

        @Override
        public AssessmentResultResponseDto getLatestAssessmentResult(Long userId) {
            if (savedAssessmentScore == null || !userId.equals(savedUserId)) {
                return null;
            }

            AssessmentResultResponseDto response = new AssessmentResultResponseDto();
            response.setResultId(1L);
            response.setUserId(savedUserId);
            response.setRtScore(savedAssessmentScore.getRtScore());
            response.setLhScore(savedAssessmentScore.getLhScore());
            response.setRpScore(savedAssessmentScore.getRpScore());
            return response;
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
            savedUserId = userId;
            savedAssessmentScore = assessmentScore;
            return 1;
        }
    }
}
