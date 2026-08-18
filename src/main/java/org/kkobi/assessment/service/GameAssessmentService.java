package org.kkobi.assessment.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.calculator.GameBehaviorAssessmentCalculator;
import org.kkobi.assessment.calculator.GameScoreCalculator;
import org.kkobi.assessment.domain.AssessmentResult;
import org.kkobi.assessment.domain.AssessmentResultDetails;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.domain.BehaviorAnalysisResult;
import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.dto.GameCompletionResponse;
import org.kkobi.game.dto.ScenarioDto;
import org.kkobi.game.service.ActionLogService;
import org.kkobi.game.service.ScenarioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameAssessmentService {

    private static final String GAME_SCENARIO_ID = "SC001";

    private final ActionLogService actionLogService;
    private final ScenarioService scenarioService;
    private final GameBehaviorAssessmentCalculator gameBehaviorAssessmentCalculator;
    private final GameScoreCalculator gameScoreCalculator;
    private final AssessmentResultService assessmentResultService;

    public boolean existsCompletedGame(Long userId) {
        return actionLogService.existsCompletedGame(userId);
    }

    @Transactional
    public GameCompletionResponse completeGame(Long userId) {
        validateUserId(userId);
        actionLogService.lockGameUser(userId);
        validateGameCompletion(userId);
        AssessmentResult assessmentResult = calculateGameAssessment(userId);
        AssessmentResultDetails resultDetails = assessmentResultService
                .getLatestAssessmentResultDetails(userId);
        return new GameCompletionResponse(assessmentResult, resultDetails);
    }

    @Transactional
    public AssessmentResult calculateGameAssessment(Long userId) {
        List<ActionLogDto> actionLogs = actionLogService.getActionLogsByUserId(userId);
        ScenarioDto scenario = scenarioService.getScenario(GAME_SCENARIO_ID);
        BehaviorAnalysisResult gameAnalysis = gameBehaviorAssessmentCalculator.calculate(
                scenario,
                actionLogs
        );
        AssessmentScore assessmentScore = gameScoreCalculator.calculateGameScore(
                List.of(gameAnalysis.getTotalScoreDelta())
        );

        return assessmentResultService.saveAssessmentResult(
                userId,
                assessmentScore,
                gameAnalysis.getAppliedRules()
        );
    }

    private void validateGameCompletion(Long userId) {
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

    private void validateUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }
    }

}
