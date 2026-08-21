package org.kkobi.goal.mapper;

import org.apache.ibatis.annotations.Param;
import org.kkobi.goal.domain.FinancialGoal;

public interface FinancialGoalMapper {
    FinancialGoal findByUserId(@Param("userId") Long userId);

    int upsert(FinancialGoal financialGoal);

    int deleteByUserId(@Param("userId") Long userId);
}
