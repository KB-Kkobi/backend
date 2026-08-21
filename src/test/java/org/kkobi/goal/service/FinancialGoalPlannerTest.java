package org.kkobi.goal.service;

import org.junit.jupiter.api.Test;
import org.kkobi.goal.domain.FinancialGoal;
import org.kkobi.goal.enums.FinancialGoalType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialGoalPlannerTest {

    private final FinancialGoalPlanner planner = new FinancialGoalPlanner();

    @Test
    void subtractsCurrentAmountAndUsesExactTerm() {
        FinancialGoalPlan plan = planner.createPlan(
                createGoal(3_000_000L, 600_000L, 12),
                List.of(6, 12, 24)
        );

        assertEquals(2_400_000L, plan.remainingAmount());
        assertEquals(200_000L, plan.monthlyReferenceAmount());
        assertEquals(12, plan.recommendedSavingTerm());
        assertTrue(plan.goalMatched());
        assertTrue(plan.recommendationReason().contains("12개월 상품"));
    }

    @Test
    void choosesLongestAvailableTermWithinTarget() {
        FinancialGoalPlan plan = planner.createPlan(
                createGoal(3_000_000L, 0L, 10),
                List.of(6, 12, 24)
        );

        assertEquals(6, plan.recommendedSavingTerm());
    }

    @Test
    void fallsBackToGeneralComparisonWhenAllTermsExceedTarget() {
        FinancialGoalPlan plan = planner.createPlan(
                createGoal(3_000_000L, 0L, 3),
                List.of(6, 12, 24)
        );

        assertFalse(plan.goalMatched());
        assertEquals(null, plan.recommendedSavingTerm());
        assertTrue(plan.recommendationReason().contains("가입 기간 제한 없이"));
    }

    @Test
    void roundsMonthlyReferenceAmountUp() {
        FinancialGoalPlan plan = planner.createPlan(
                createGoal(100L, 0L, 3),
                List.of(3)
        );

        assertEquals(34L, plan.monthlyReferenceAmount());
    }

    @Test
    void fullyFundedGoalHasNoRemainingOrMonthlyAmount() {
        FinancialGoalPlan plan = planner.createPlan(
                createGoal(3_000_000L, 3_500_000L, 12),
                List.of(12)
        );

        assertEquals(0L, plan.remainingAmount());
        assertEquals(0L, plan.monthlyReferenceAmount());
        assertTrue(plan.recommendationReason().contains("이미 준비"));
    }

    private FinancialGoal createGoal(
            long targetAmount,
            long currentAmount,
            int targetMonths
    ) {
        FinancialGoal goal = new FinancialGoal();
        goal.setGoalType(FinancialGoalType.TRAVEL);
        goal.setTargetAmount(targetAmount);
        goal.setCurrentAmount(currentAmount);
        goal.setTargetMonths(targetMonths);
        return goal;
    }
}
