package org.kkobi.assessment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.calculator.BehaviorRuleEngine;
import org.kkobi.assessment.calculator.GameScoreCalculator;
import org.kkobi.assessment.calculator.PersonaClassifier;
import org.kkobi.assessment.domain.AssessmentResult;
import org.kkobi.assessment.domain.AssessmentResultDetails;
import org.kkobi.assessment.domain.AssessmentScore;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.mapper.AssessmentMapper;
import org.kkobi.game.dto.ActionLogDto;
import org.kkobi.game.dto.GameCompletionResponse;
import org.kkobi.game.mapper.ActionLogMapper;
import org.kkobi.game.service.ActionLogService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameAssessmentServiceTest {

    @Test
    @DisplayName("게임 행동 로그를 계산하여 최종 성향 결과를 반환한다.")
    void completeGameReturnsCalculatedAssessmentResult() {
        AssessmentMapper assessmentMapper = createCompletionAssessmentMapper();
        GameAssessmentService gameAssessmentService = createGameAssessmentService(
                List.of(createActionLog("INITIAL_ALLOCATION", 0L)),
                assessmentMapper
        );

        GameCompletionResponse response = gameAssessmentService.completeGame(1L);

        assertEquals(1024L, response.getResultId());
        assertEquals("GAME", response.getAssessmentSource());
        assertEquals("HHH", response.getPersonaCode());
        assertEquals("불꽃 추격자", response.getPersonaName());
        assertScoreEquals("50.00", response.getScores().getRtScore());
        assertScoreEquals("50.00", response.getScores().getLhScore());
        assertScoreEquals("50.00", response.getScores().getRpScore());
        assertScoreEquals("70.00", response.getRecommendedRatio().getStockRatio());
    }

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
            public AssessmentResultDetails getLatestAssessmentResultDetails(Long userId) {
                return null;
            }

            @Override
            public Long getPersonaIdByAxisCode(String axisCode) {
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

    private AssessmentMapper createCompletionAssessmentMapper() {
        return new AssessmentMapper() {

            private AssessmentScore savedScore;

            @Override
            public AssessmentScore getLatestAssessmentScore(Long userId) {
                return savedScore;
            }

            @Override
            public AssessmentResultDetails getLatestAssessmentResultDetails(Long userId) {
                AssessmentResultDetails resultDetails = new AssessmentResultDetails();
                resultDetails.setResultId(1024L);
                resultDetails.setPersonaName("불꽃 추격자");
                resultDetails.setDescription("적극적으로 수익을 추구합니다.");
                resultDetails.setFeature("시장 변화에 적극적으로 대응합니다.");
                resultDetails.setStrength("분산 투자 전략을 활용합니다.");
                resultDetails.setCaution("과도한 위험을 피해야 합니다.");
                resultDetails.setStockRatio(new BigDecimal("70.00"));
                resultDetails.setBondRatio(new BigDecimal("20.00"));
                resultDetails.setDepositRatio(new BigDecimal("10.00"));
                resultDetails.setAnalyzedAt(LocalDateTime.of(2026, 8, 6, 15, 32, 10));
                return resultDetails;
            }

            @Override
            public Long getPersonaIdByAxisCode(String axisCode) {
                return 1L;
            }

            @Override
            public int saveAssessmentResult(
                    Long userId,
                    Long personaId,
                    AssessmentScore assessmentScore) {
                savedScore = assessmentScore;
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
