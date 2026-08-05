package org.kkobi.assessment.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.calculator.VirtualInvestmentScoreCalculator;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.domain.BehaviorAnalysisResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VirtualInvestmentPeriodResultService {

    private final VirtualInvestmentScoreCalculator virtualInvestmentScoreCalculator;
    private final AssessmentResultService assessmentResultService;

    @Transactional
    public void saveVirtualInvestmentPeriodResult(
            Long userId,
            BehaviorAnalysisResult analysisResult) {
        AssessmentScore currentScore = assessmentResultService.getLatestAssessmentScore(userId);
        AssessmentScore updatedScore = virtualInvestmentScoreCalculator.calculateVirtualInvestmentScore(
                currentScore,
                analysisResult.getTotalScoreDelta()
        );
        assessmentResultService.saveAssessmentResult(
                userId,
                updatedScore,
                analysisResult.getAppliedRules()
        );
    }
}
