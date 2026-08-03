package org.kkobi.assessment.calculator;

import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.domain.ScoreDelta;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class GameScoreCalculator {

    private static final BigDecimal MINIMUM_SCORE = BigDecimal.ZERO;
    private static final BigDecimal MAXIMUM_SCORE = BigDecimal.valueOf(100);
    private static final int SCORE_SCALE = 2;

    public AssessmentScore calculateGameScore(List<ScoreDelta> scoreDeltas) {
        ScoreDelta totalScoreDelta = scoreDeltas.stream()
                .reduce(ScoreDelta.createZeroScoreDelta(), ScoreDelta::addScoreDelta);
        AssessmentScore initialScore = AssessmentScore.createInitialScore();

        return new AssessmentScore(
                calculateScore(initialScore.getRtScore(), totalScoreDelta.getRtDelta()),
                calculateScore(initialScore.getLhScore(), totalScoreDelta.getLhDelta()),
                calculateScore(initialScore.getRpScore(), totalScoreDelta.getRpDelta())
        );
    }

    private BigDecimal calculateScore(BigDecimal initialScore, BigDecimal scoreDelta) {
        return clampScore(initialScore.add(scoreDelta));
    }

    private BigDecimal clampScore(BigDecimal score) {
        return score.max(MINIMUM_SCORE)
                .min(MAXIMUM_SCORE)
                .setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }
}
