package org.kkobi.assessment.service;

import lombok.RequiredArgsConstructor;
import org.kkobi.assessment.calculator.GameScoreCalculator;
import org.kkobi.assessment.domain.AssessmentResult;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.domain.ScoreDelta;
import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.service.ActionLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GameAssessmentService {

    private final ActionLogService actionLogService;
    private final GameScoreCalculator gameScoreCalculator;
    private final AssessmentResultService assessmentResultService;

    @Transactional
    public AssessmentResult calculateGameAssessment(Long userId) {
        List<ScoreDelta> scoreDeltas = actionLogService.getActionLogsByUserId(userId)
                .stream()
                .map(this::createScoreDelta)
                .toList();
        AssessmentScore assessmentScore = gameScoreCalculator.calculateGameScore(scoreDeltas);

        return assessmentResultService.saveAssessmentResult(
                userId,
                assessmentScore,
                List.of()
        );
    }

    private ScoreDelta createScoreDelta(ActionLogDto actionLog) {
        return new ScoreDelta(
                actionLog.getRtScoreDelta(),
                actionLog.getLhScoreDelta(),
                actionLog.getRpScoreDelta()
        );
    }
}
