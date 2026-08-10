package org.kkobi.game.dto;

import lombok.Getter;
import org.kkobi.assessment.domain.AssessmentResult;
import org.kkobi.assessment.domain.AssessmentResultDetails;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class GameCompletionResponse {

    private static final String ASSESSMENT_SOURCE = "GAME";

    private final Long resultId;
    private final String assessmentSource;
    private final String personaCode;
    private final String personaName;
    private final String description;
    private final String investmentFeature;
    private final String doAndDont;
    private final String recommendedStrategy;
    private final Scores scores;
    private final RecommendedRatio recommendedRatio;
    private final LocalDateTime analyzedAt;

    public GameCompletionResponse(
            AssessmentResult assessmentResult,
            AssessmentResultDetails resultDetails) {
        this.resultId = resultDetails.getResultId();
        this.assessmentSource = ASSESSMENT_SOURCE;
        this.personaCode = assessmentResult.getPersonaType().getAxisCode();
        this.personaName = resultDetails.getPersonaName();
        this.description = resultDetails.getDescription();
        this.investmentFeature = resultDetails.getFeature();
        this.doAndDont = resultDetails.getCaution();
        this.recommendedStrategy = resultDetails.getStrength();
        this.scores = new Scores(
                assessmentResult.getAssessmentScore().getRtScore(),
                assessmentResult.getAssessmentScore().getLhScore(),
                assessmentResult.getAssessmentScore().getRpScore()
        );
        this.recommendedRatio = new RecommendedRatio(
                resultDetails.getStockRatio(),
                resultDetails.getBondRatio(),
                resultDetails.getDepositRatio()
        );
        this.analyzedAt = resultDetails.getAnalyzedAt();
    }

    @Getter
    public static class Scores {

        private final BigDecimal rtScore;
        private final BigDecimal lhScore;
        private final BigDecimal rpScore;

        public Scores(BigDecimal rtScore, BigDecimal lhScore, BigDecimal rpScore) {
            this.rtScore = rtScore;
            this.lhScore = lhScore;
            this.rpScore = rpScore;
        }
    }

    @Getter
    public static class RecommendedRatio {

        private final BigDecimal stockRatio;
        private final BigDecimal bondRatio;
        private final BigDecimal depositRatio;

        public RecommendedRatio(
                BigDecimal stockRatio,
                BigDecimal bondRatio,
                BigDecimal depositRatio) {
            this.stockRatio = stockRatio;
            this.bondRatio = bondRatio;
            this.depositRatio = depositRatio;
        }
    }
}
