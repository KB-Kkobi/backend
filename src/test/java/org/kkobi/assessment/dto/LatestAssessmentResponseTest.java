package org.kkobi.assessment.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.domain.AssessmentResultDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatestAssessmentResponseTest {

    @Test
    @DisplayName("최신 성향 결과를 조회 응답으로 변환한다.")
    void createLatestAssessmentResponse() {
        AssessmentResultDetails resultDetails = createAssessmentResultDetails();

        LatestAssessmentResponse response = new LatestAssessmentResponse(resultDetails);

        assertEquals(12L, response.getResultId());
        assertEquals("HLH", response.getPersonaCode());
        assertEquals("야망찬 개척자", response.getTypeName());
        assertEquals(new BigDecimal("82.50"), response.getScores().getRtScore());
        assertEquals(new BigDecimal("35.00"), response.getScores().getLhScore());
        assertEquals(new BigDecimal("78.00"), response.getScores().getRpScore());
        assertEquals(new BigDecimal("70.00"), response.getRecommendedRatio().getStockRatio());
        assertEquals(new BigDecimal("20.00"), response.getRecommendedRatio().getBondRatio());
        assertEquals(new BigDecimal("10.00"), response.getRecommendedRatio().getDepositRatio());
    }

    private AssessmentResultDetails createAssessmentResultDetails() {
        AssessmentResultDetails resultDetails = new AssessmentResultDetails();
        resultDetails.setResultId(12L);
        resultDetails.setAxisCode("HLH");
        resultDetails.setPersonaName("야망찬 개척자");
        resultDetails.setDescription("설명");
        resultDetails.setFeature("투자 특징");
        resultDetails.setStrength("추천 전략");
        resultDetails.setCaution("주의사항");
        resultDetails.setRtScore(new BigDecimal("82.50"));
        resultDetails.setLhScore(new BigDecimal("35.00"));
        resultDetails.setRpScore(new BigDecimal("78.00"));
        resultDetails.setStockRatio(new BigDecimal("70.00"));
        resultDetails.setBondRatio(new BigDecimal("20.00"));
        resultDetails.setDepositRatio(new BigDecimal("10.00"));
        resultDetails.setAnalyzedAt(LocalDateTime.of(2026, 8, 6, 15, 32, 10));
        return resultDetails;
    }
}
