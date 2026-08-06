package org.kkobi.assessment.domain;

import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class ScoreDelta {

    private static final int SCORE_SCALE = 2;

    private final BigDecimal rtDelta;
    private final BigDecimal lhDelta;
    private final BigDecimal rpDelta;

    public ScoreDelta(BigDecimal rtDelta, BigDecimal lhDelta, BigDecimal rpDelta) {
        this.rtDelta = rtDelta;
        this.lhDelta = lhDelta;
        this.rpDelta = rpDelta;
    }

    public static ScoreDelta createScoreDelta(int rtDelta, int lhDelta, int rpDelta) {
        return new ScoreDelta(
                BigDecimal.valueOf(rtDelta),
                BigDecimal.valueOf(lhDelta),
                BigDecimal.valueOf(rpDelta)
        );
    }

    public static ScoreDelta createZeroScoreDelta() {
        return createScoreDelta(0, 0, 0);
    }

    public ScoreDelta addScoreDelta(ScoreDelta scoreDelta) {
        return new ScoreDelta(
                rtDelta.add(scoreDelta.rtDelta),
                lhDelta.add(scoreDelta.lhDelta),
                rpDelta.add(scoreDelta.rpDelta)
        );
    }

    public ScoreDelta multiplyScoreDelta(BigDecimal multiplier) {
        return new ScoreDelta(
                multiplyScore(rtDelta, multiplier),
                multiplyScore(lhDelta, multiplier),
                multiplyScore(rpDelta, multiplier)
        );
    }

    private BigDecimal multiplyScore(BigDecimal score, BigDecimal multiplier) {
        return score.multiply(multiplier).setScale(SCORE_SCALE, RoundingMode.HALF_UP);
    }
}
