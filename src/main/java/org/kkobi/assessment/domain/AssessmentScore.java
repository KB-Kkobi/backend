package org.kkobi.assessment.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentScore {

    private BigDecimal rtScore;
    private BigDecimal lhScore;
    private BigDecimal rpScore;

    public static AssessmentScore createInitialScore() {
        BigDecimal initialScore = BigDecimal.valueOf(50);
        return new AssessmentScore(initialScore, initialScore, initialScore);
    }
}
