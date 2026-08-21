package org.kkobi.goal.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.kkobi.goal.enums.FinancialGoalType;

import java.time.LocalDateTime;

@Getter
@Builder
public class FinancialGoalResponseDto {
    private final Long financialGoalId;
    private final FinancialGoalType goalType;
    private final String customGoalName;
    private final String goalName;
    private final Long targetAmount;
    private final Long currentAmount;
    private final Long remainingAmount;
    private final Integer targetMonths;
    private final Long monthlyReferenceAmount;
    private final Integer recommendedSavingTerm;
    private final boolean goalMatched;
    private final String recommendationReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
}
