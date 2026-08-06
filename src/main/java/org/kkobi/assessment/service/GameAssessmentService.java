package org.kkobi.assessment.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.calculator.BehaviorRuleEngine;
import org.kkobi.assessment.calculator.GameScoreCalculator;
import org.kkobi.assessment.domain.AssessmentResult;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.domain.BehaviorAnalysisResult;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.domain.ScoreDelta;
import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.service.ActionLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameAssessmentService {

    private final ActionLogService actionLogService;
    private final BehaviorRuleEngine behaviorRuleEngine;
    private final GameScoreCalculator gameScoreCalculator;
    private final AssessmentResultService assessmentResultService;

    public boolean existsCompletedGame(Long userId) {
        return assessmentResultService.existsAssessmentResult(userId);
    }

    @Transactional
    public AssessmentResult calculateGameAssessment(Long userId) {
        List<ActionLogDto> actionLogs = actionLogService.getActionLogsByUserId(userId);
        List<ScoreDelta> scoreDeltas = new ArrayList<>(actionLogs
                .stream()
                .map(this::createScoreDelta)
                .toList());
        BehaviorAnalysisResult gameCompletionAnalysis = calculateGameCompletionAnalysis(actionLogs);
        scoreDeltas.add(gameCompletionAnalysis.getTotalScoreDelta());
        AssessmentScore assessmentScore = gameScoreCalculator.calculateGameScore(scoreDeltas);

        return assessmentResultService.saveAssessmentResult(
                userId,
                assessmentScore,
                gameCompletionAnalysis.getAppliedRules()
        );
    }

    private BehaviorAnalysisResult calculateGameCompletionAnalysis(List<ActionLogDto> actionLogs) {
        BehaviorContext gameCompletionContext = new BehaviorContext();
        gameCompletionContext.setDepositMatured(existsMaturedDeposit(actionLogs));
        return behaviorRuleEngine.calculateBehaviorAnalysis(gameCompletionContext);
    }

    private boolean existsMaturedDeposit(List<ActionLogDto> actionLogs) {
        boolean existsInitialDeposit = actionLogs.stream()
                .filter(actionLog -> "INITIAL_ALLOCATION".equals(actionLog.getActionType()))
                .map(ActionLogDto::getCurrentDeposit)
                .filter(currentDeposit -> currentDeposit != null && currentDeposit > 0)
                .findFirst()
                .isPresent();
        boolean existsDepositCancel = actionLogs.stream()
                .anyMatch(actionLog -> "DEPOSIT_CANCEL".equals(actionLog.getActionType()));
        return existsInitialDeposit && !existsDepositCancel;
    }

    private ScoreDelta createScoreDelta(ActionLogDto actionLog) {
        return new ScoreDelta(
                actionLog.getRtScoreDelta(),
                actionLog.getLhScoreDelta(),
                actionLog.getRpScoreDelta()
        );
    }
}
