package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreAxisCorrelationAnalysisTest {

    @Test
    void calculatePositiveAndNegativeAxisCorrelations() {
        ScoreAxisCorrelationAnalysis analysis = new ScoreAxisCorrelationAnalysis();
        analysis.addScores(new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("30"));
        analysis.addScores(new BigDecimal("20"), new BigDecimal("20"), new BigDecimal("20"));
        analysis.addScores(new BigDecimal("30"), new BigDecimal("30"), new BigDecimal("10"));

        assertEquals(new BigDecimal("-1.0000"), analysis.getRtRpCorrelation());
        assertEquals(new BigDecimal("1.0000"), analysis.getRtLhCorrelation());
        assertEquals(new BigDecimal("-1.0000"), analysis.getLhRpCorrelation());
        assertEquals(3, analysis.getSampleCount());
    }
}
