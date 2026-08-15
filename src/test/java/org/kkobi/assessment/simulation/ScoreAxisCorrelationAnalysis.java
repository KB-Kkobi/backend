package org.kkobi.assessment.simulation;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ScoreAxisCorrelationAnalysis {

    private static final int CORRELATION_SCALE = 4;

    private final PearsonCorrelation rtRpCorrelation = new PearsonCorrelation();
    private final PearsonCorrelation rtLhCorrelation = new PearsonCorrelation();
    private final PearsonCorrelation lhRpCorrelation = new PearsonCorrelation();

    public void addResult(GameBehaviorSimulationResult simulationResult) {
        if (simulationResult == null) {
            throw new IllegalArgumentException("시뮬레이션 결과는 필수입니다.");
        }
        addScores(
                simulationResult.getFinalRtScore(),
                simulationResult.getFinalLhScore(),
                simulationResult.getFinalRpScore()
        );
    }

    public void addScores(BigDecimal rtScore, BigDecimal lhScore, BigDecimal rpScore) {
        if (rtScore == null || lhScore == null || rpScore == null) {
            throw new IllegalArgumentException("RT·LH·RP 점수는 모두 필수입니다.");
        }
        rtRpCorrelation.add(rtScore.doubleValue(), rpScore.doubleValue());
        rtLhCorrelation.add(rtScore.doubleValue(), lhScore.doubleValue());
        lhRpCorrelation.add(lhScore.doubleValue(), rpScore.doubleValue());
    }

    public long getSampleCount() {
        return rtRpCorrelation.getSampleCount();
    }

    public BigDecimal getRtRpCorrelation() {
        return rtRpCorrelation.calculate();
    }

    public BigDecimal getRtLhCorrelation() {
        return rtLhCorrelation.calculate();
    }

    public BigDecimal getLhRpCorrelation() {
        return lhRpCorrelation.calculate();
    }

    private static class PearsonCorrelation {

        private long sampleCount;
        private double xAverage;
        private double yAverage;
        private double xSquaredDeviationSum;
        private double ySquaredDeviationSum;
        private double coDeviationSum;

        private void add(double xValue, double yValue) {
            sampleCount++;
            double xDifference = xValue - xAverage;
            xAverage += xDifference / sampleCount;
            double yDifference = yValue - yAverage;
            yAverage += yDifference / sampleCount;
            xSquaredDeviationSum += xDifference * (xValue - xAverage);
            ySquaredDeviationSum += yDifference * (yValue - yAverage);
            coDeviationSum += xDifference * (yValue - yAverage);
        }

        private long getSampleCount() {
            return sampleCount;
        }

        private BigDecimal calculate() {
            if (sampleCount < 2
                    || xSquaredDeviationSum == 0
                    || ySquaredDeviationSum == 0) {
                return BigDecimal.ZERO.setScale(CORRELATION_SCALE);
            }
            double correlation = coDeviationSum
                    / Math.sqrt(xSquaredDeviationSum * ySquaredDeviationSum);
            double boundedCorrelation = Math.max(-1, Math.min(1, correlation));
            return BigDecimal.valueOf(boundedCorrelation)
                    .setScale(CORRELATION_SCALE, RoundingMode.HALF_UP);
        }
    }
}
