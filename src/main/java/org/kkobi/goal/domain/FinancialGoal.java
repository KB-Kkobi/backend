package org.kkobi.goal.domain;

import lombok.Data;
import org.kkobi.goal.enums.FinancialGoalType;

import java.time.LocalDateTime;

@Data
public class FinancialGoal {
    private Long financialGoalId;
    private Long userId;
    private FinancialGoalType goalType;
    private String customGoalName;
    private Long targetAmount;
    private Long currentAmount;
    private Integer targetMonths;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
