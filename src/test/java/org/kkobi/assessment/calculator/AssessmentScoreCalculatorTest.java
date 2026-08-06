package org.kkobi.assessment.calculator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.domain.ScoreDelta;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AssessmentScoreCalculatorTest {

    private final GameScoreCalculator gameScoreCalculator = new GameScoreCalculator();
    private final VirtualInvestmentScoreCalculator virtualInvestmentScoreCalculator =
            new VirtualInvestmentScoreCalculator();

    @Test
    @DisplayName("게임 점수는 50점에 변화량을 더하고 0부터 100 사이로 제한한다.")
    void calculateGameScore() {
        AssessmentScore assessmentScore = gameScoreCalculator.calculateGameScore(
                List.of(ScoreDelta.createScoreDelta(60, -70, 5))
        );

        assertScoreEquals("100.00", assessmentScore.getRtScore());
        assertScoreEquals("0.00", assessmentScore.getLhScore());
        assertScoreEquals("55.00", assessmentScore.getRpScore());
    }

    @Test
    @DisplayName("가상투자 점수는 행동 점수를 변환해 EMA를 적용하고 근거 없는 축은 유지한다.")
    void calculateVirtualInvestmentScore() {
        AssessmentScore assessmentScore = virtualInvestmentScoreCalculator
                .calculateVirtualInvestmentScore(
                        AssessmentScore.createInitialScore(),
                        ScoreDelta.createScoreDelta(10, -5, 0)
                );

        assertScoreEquals("53.33", assessmentScore.getRtScore());
        assertScoreEquals("48.34", assessmentScore.getLhScore());
        assertScoreEquals("50.00", assessmentScore.getRpScore());
    }

    private void assertScoreEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
