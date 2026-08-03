package org.kkobi.assessment.calculator;

import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.domain.ScoreDelta;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class VirtualInvestmentScoreCalculator {

    private static final BigDecimal MINIMUM_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAXIMUM_SCORE = BigDecimal.valueOf(100);
    private static final BigDecimal NEUTRAL_SCORE = BigDecimal.valueOf(50);
    private static final BigDecimal DELTA_CONVERSION_RATE = BigDecimal.valueOf(3.33);
    private static final BigDecimal PREVIOUS_SCORE_WEIGHT = BigDecimal.valueOf(0.9);
    private static final BigDecimal BEHAVIOR_SCORE_WEIGHT = BigDecimal.valueOf(0.1);
    private static final int SCORE_SCALE = 2;

    public AssessmentScore calculateVirtualInvestmentScore(
            AssessmentScore currentScore,
            ScoreDelta scoreDelta) {
        return new AssessmentScore(
                calculateEmaScore(currentScore.getRtScore(), scoreDelta.getRtDelta()),
                calculateEmaScore(currentScore.getLhScore(), scoreDelta.getLhDelta()),
                calculateEmaScore(currentScore.getRpScore(), scoreDelta.getRpDelta())
        );
    }

    private BigDecimal calculateEmaScore(BigDecimal currentScore, BigDecimal scoreDelta) {
        if (scoreDelta.signum() == 0) {
            return currentScore.setScale(SCORE_SCALE, RoundingMode.HALF_UP);
        }

        BigDecimal behaviorScore = clampScore(
                NEUTRAL_SCORE.add(scoreDelta.multiply(DELTA_CONVERSION_RATE))
        );
        BigDecimal updatedScore = currentScore.multiply(PREVIOUS_SCORE_WEIGHT)
                .add(behaviorScore.multiply(BEHAVIOR_SCORE_WEIGHT));

        return clampScore(updatedScore);
    }

    private BigDecimal clampScore(BigDecimal score) {
        return score.max(MINIMUM_SCORE)
                .min(MAXIMUM_SCORE)
                .setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }
}
