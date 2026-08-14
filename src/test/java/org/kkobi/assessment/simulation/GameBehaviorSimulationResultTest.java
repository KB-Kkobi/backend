package org.kkobi.assessment.simulation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kkobi.assessment.enums.BehaviorRuleCode;
import org.kkobi.assessment.enums.PersonaType;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameBehaviorSimulationResultTest {

    @Test
    @DisplayName("게임 행동 지표와 최종 성향 결과를 생성한다.")
    void createGameBehaviorSimulationResult() {
        GameBehaviorSimulationResult result = createResult();

        assertEquals(5, result.getTradeCount());
        assertEquals(new BigDecimal("2.00"), result.getRtBoundaryDistance());
        assertEquals(new BigDecimal("6.00"), result.getLhBoundaryDistance());
        assertEquals(new BigDecimal("12.00"), result.getRpBoundaryDistance());
        assertTrue(result.isNearBoundary(BigDecimal.valueOf(5)));
        assertFalse(result.isNearBoundary(BigDecimal.ONE));
        assertEquals(1, result.getRuleApplicationCounts().get(BehaviorRuleCode.CRASH_BUY));
        assertEquals(PersonaType.HLH, result.getPersonaType());
    }

    @Test
    @DisplayName("초기 자산 배분 비율의 합이 100이 아니면 생성할 수 없다.")
    void rejectInvalidInitialAllocationRatio() {
        assertThrows(IllegalArgumentException.class, () -> createResultBuilder()
                .initialCashRatio(BigDecimal.valueOf(20))
                .initialStockRatio(BigDecimal.valueOf(40))
                .initialDepositRatio(BigDecimal.valueOf(30))
                .build());
    }

    @Test
    @DisplayName("예금 해지와 만기 유지는 동시에 기록할 수 없다.")
    void rejectConflictingDepositStatus() {
        assertThrows(IllegalArgumentException.class, () -> createResultBuilder()
                .depositCancelled(true)
                .depositMatured(true)
                .build());
    }

    @Test
    @DisplayName("행동 규칙 적용 횟수는 생성 후 변경할 수 없다.")
    void protectRuleApplicationCounts() {
        GameBehaviorSimulationResult result = createResult();

        assertThrows(UnsupportedOperationException.class, () ->
                result.getRuleApplicationCounts().put(BehaviorRuleCode.BULL_BUY, 1)
        );
    }

    private GameBehaviorSimulationResult createResult() {
        return createResultBuilder().build();
    }

    private GameBehaviorSimulationResult.GameBehaviorSimulationResultBuilder createResultBuilder() {
        return GameBehaviorSimulationResult.builder()
                .simulationUserId(1L)
                .initialCashRatio(BigDecimal.valueOf(20))
                .initialStockRatio(BigDecimal.valueOf(50))
                .initialDepositRatio(BigDecimal.valueOf(30))
                .buyCount(3)
                .sellCount(2)
                .noActionTickCount(47)
                .totalBuyAmount(2_000_000L)
                .totalSellAmount(1_000_000L)
                .fullSellCount(1)
                .crashBuyCount(1)
                .crashFullSellCount(0)
                .bullBuyCount(1)
                .bullProfitSellCount(1)
                .lossAveragingBuyCount(0)
                .lossCutSellCount(1)
                .depositCancelled(true)
                .depositMatured(false)
                .boughtStockAfterDepositCancel(true)
                .maximumConsecutiveBuyCount(2)
                .maximumConsecutiveSellCount(1)
                .ruleApplicationCounts(Map.of(
                        BehaviorRuleCode.CRASH_BUY,
                        1,
                        BehaviorRuleCode.LOSS_CUT_SELL,
                        1
                ))
                .finalRtScore(new BigDecimal("52.00"))
                .finalLhScore(new BigDecimal("44.00"))
                .finalRpScore(new BigDecimal("62.00"))
                .personaType(PersonaType.HLH);
    }
}
