package org.kkobi.goal.service;

public record FinancialGoalPlan(
        long remainingAmount,
        long monthlyReferenceAmount,
        Integer recommendedSavingTerm,
        boolean goalMatched,
        String recommendationReason
) {
}
