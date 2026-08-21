package org.kkobi.goal.service;

import org.kkobi.goal.domain.FinancialGoal;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class FinancialGoalPlanner {

    public FinancialGoalPlan createPlan(
            FinancialGoal goal,
            List<Integer> availableSavingTerms
    ) {
        long remainingAmount = calculateRemainingAmount(
                goal.getTargetAmount(),
                goal.getCurrentAmount()
        );
        Integer savingTerm = findRecommendedSavingTerm(
                goal.getTargetMonths(),
                availableSavingTerms
        );
        boolean goalMatched = savingTerm != null;

        return new FinancialGoalPlan(
                remainingAmount,
                calculateMonthlyReferenceAmount(
                        remainingAmount,
                        goal.getTargetMonths()
                ),
                savingTerm,
                goalMatched,
                buildRecommendationReason(goal, remainingAmount, savingTerm)
        );
    }

    Integer findRecommendedSavingTerm(
            int targetMonths,
            List<Integer> availableSavingTerms
    ) {
        if (availableSavingTerms == null) {
            return null;
        }

        return availableSavingTerms.stream()
                .filter(term -> term != null && term > 0 && term <= targetMonths)
                .max(Integer::compareTo)
                .orElse(null);
    }

    private long calculateRemainingAmount(long targetAmount, long currentAmount) {
        return Math.max(targetAmount - currentAmount, 0L);
    }

    private long calculateMonthlyReferenceAmount(long remainingAmount, int targetMonths) {
        return BigDecimal.valueOf(remainingAmount)
                .divide(BigDecimal.valueOf(targetMonths), 0, RoundingMode.CEILING)
                .longValue();
    }

    private String buildRecommendationReason(
            FinancialGoal goal,
            long remainingAmount,
            Integer savingTerm
    ) {
        if (remainingAmount == 0) {
            return "목표 금액만큼 이미 준비되어 있어요. 목표 시점에 맞는 상품을 확인해 보세요.";
        }

        if (savingTerm == null) {
            return "목표 시점 전에 만기가 오는 상품이 없어 가입 기간 제한 없이 보여드려요.";
        }

        return formatDuration(goal.getTargetMonths()) + " 뒤 필요한 자금이라 "
                + savingTerm + "개월 상품부터 보여드려요.";
    }

    private String formatDuration(int months) {
        if (months % 12 == 0) {
            return (months / 12) + "년";
        }
        return months + "개월";
    }
}
