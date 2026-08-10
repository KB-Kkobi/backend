package org.kkobi.assessment.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.calculator.BehaviorRuleEngine;
import org.kkobi.assessment.calculator.GameScoreCalculator;
import org.kkobi.assessment.domain.AssessmentResult;
import org.kkobi.assessment.domain.AssessmentResultDetails;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.domain.BehaviorAnalysisResult;
import org.kkobi.assessment.domain.BehaviorContext;
import org.kkobi.assessment.domain.ScoreDelta;
import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.dto.GameCompletionResponse;
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
    public GameCompletionResponse completeGame(Long userId) {
        validateGameCompletion(userId);
        AssessmentResult assessmentResult = calculateGameAssessment(userId);
        AssessmentResultDetails resultDetails = assessmentResultService
                .getLatestAssessmentResultDetails(userId);
        return new GameCompletionResponse(assessmentResult, resultDetails);
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

    private void validateGameCompletion(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
        if (existsCompletedGame(userId)) {
            throw new IllegalStateException("이미 완료된 게임입니다.");
        }
        boolean existsInitialAllocation = actionLogService.getActionLogsByUserId(userId)
                .stream()
                .anyMatch(actionLog -> "INITIAL_ALLOCATION".equals(actionLog.getActionType()));
        if (!existsInitialAllocation) {
            throw new IllegalStateException("게임 시작 기록을 찾을 수 없습니다.");
        }
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
