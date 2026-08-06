package org.kkobi.assessment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.calculator.BehaviorRuleEngine;
import org.kkobi.assessment.calculator.GameScoreCalculator;
import org.kkobi.assessment.calculator.PersonaClassifier;
import org.kkobi.assessment.domain.AssessmentResult;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.mapper.AssessmentMapper;
import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.mapper.ActionLogMapper;
import org.kkobi.game.service.ActionLogService;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameAssessmentServiceTest {

    @Test
    @DisplayName("저장된 성향 결과가 있으면 게임을 완료한 것으로 판단한다.")
    void existsCompletedGameReturnsTrueWhenAssessmentResultExists() {
        GameAssessmentService gameAssessmentService = createGameAssessmentService(
                List.of(),
                createAssessmentMapper(AssessmentScore.createInitialScore())
        );

        assertTrue(gameAssessmentService.existsCompletedGame(1L));
    }

    @Test
    @DisplayName("저장된 성향 결과가 없으면 게임을 완료하지 않은 것으로 판단한다.")
    void existsCompletedGameReturnsFalseWhenAssessmentResultDoesNotExist() {
        GameAssessmentService gameAssessmentService = createGameAssessmentService(
                List.of(),
                createAssessmentMapper(null)
        );

        assertFalse(gameAssessmentService.existsCompletedGame(1L));
    }

    @Test
    @DisplayName("게임 종료까지 예금을 해지하지 않으면 만기 유지 점수를 반영한다.")
    void calculateGameAssessmentAppliesDepositMaturityScore() {
        GameAssessmentService gameAssessmentService = createGameAssessmentService(List.of(
                createActionLog("INITIAL_ALLOCATION", 500_000L)
        ));

        AssessmentResult result = gameAssessmentService.calculateGameAssessment(1L);

        assertScoreEquals("45.00", result.getAssessmentScore().getRtScore());
        assertScoreEquals("40.00", result.getAssessmentScore().getLhScore());
        assertScoreEquals("45.00", result.getAssessmentScore().getRpScore());
        assertTrue(result.getAppliedRules().stream()
                .anyMatch(rule -> rule.getRuleCode() == BehaviorRuleCode.DEPOSIT_MATURITY));
    }

    @Test
    @DisplayName("게임 중 예금을 해지하면 만기 유지 점수를 반영하지 않는다.")
    void calculateGameAssessmentDoesNotApplyDepositMaturityAfterCancel() {
        GameAssessmentService gameAssessmentService = createGameAssessmentService(List.of(
                createActionLog("INITIAL_ALLOCATION", 500_000L),
                createActionLog("DEPOSIT_CANCEL", 0L)
        ));

        AssessmentResult result = gameAssessmentService.calculateGameAssessment(1L);

        assertScoreEquals("50.00", result.getAssessmentScore().getRtScore());
        assertScoreEquals("50.00", result.getAssessmentScore().getLhScore());
        assertScoreEquals("50.00", result.getAssessmentScore().getRpScore());
        assertTrue(result.getAppliedRules().isEmpty());
    }

    private GameAssessmentService createGameAssessmentService(List<ActionLogDto> actionLogs) {
        return createGameAssessmentService(actionLogs, createAssessmentMapper(null));
    }

    private GameAssessmentService createGameAssessmentService(
            List<ActionLogDto> actionLogs,
            AssessmentMapper assessmentMapper) {
        ActionLogService actionLogService = new ActionLogService(new ActionLogMapper() {
            @Override
            public int saveActionLog(ActionLogDto actionLog) {
                return 1;
            }

            @Override
            public List<ActionLogDto> getActionLogsByUserId(Long userId) {
                return actionLogs;
            }

            @Override
            public int deleteActionLogsByUserId(Long userId) {
                return 0;
            }
        });
        AssessmentResultService assessmentResultService = new AssessmentResultService(
                assessmentMapper,
                new PersonaClassifier()
        );
        return new GameAssessmentService(
                actionLogService,
                new BehaviorRuleEngine(),
                new GameScoreCalculator(),
                assessmentResultService
        );
    }

    private AssessmentMapper createAssessmentMapper(AssessmentScore latestAssessmentScore) {
        return new AssessmentMapper() {
            @Override
            public AssessmentScore getLatestAssessmentScore(Long userId) {
                return latestAssessmentScore;
            }

            @Override
            public Long getPersonaIdByName(String personaName) {
                return 1L;
            }

            @Override
            public int saveAssessmentResult(
                    Long userId,
                    Long personaId,
                    AssessmentScore assessmentScore) {
                return 1;
            }
        };
    }

    private ActionLogDto createActionLog(String actionType, Long currentDeposit) {
        ActionLogDto actionLog = new ActionLogDto();
        actionLog.setActionType(actionType);
        actionLog.setCurrentDeposit(currentDeposit);
        actionLog.setRtScoreDelta(BigDecimal.ZERO);
        actionLog.setLhScoreDelta(BigDecimal.ZERO);
        actionLog.setRpScoreDelta(BigDecimal.ZERO);
        return actionLog;
    }

    private void assertScoreEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
