package org.kkobi.goal.dto.request;

import lombok.Data;
import org.kkobi.goal.enums.FinancialGoalType;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class FinancialGoalSaveRequestDto {

    @NotNull(message = "목표 유형은 필수입니다.")
    private FinancialGoalType goalType;

    @Size(max = 50, message = "기타 목표명은 50자 이하여야 합니다.")
    private String customGoalName;

    @NotNull(message = "목표 금액은 필수입니다.")
    @Min(value = 1, message = "목표 금액은 1원 이상이어야 합니다.")
    @Max(value = 1_000_000_000_000L, message = "목표 금액은 1조 원 이하여야 합니다.")
    private Long targetAmount;

    @NotNull(message = "현재 마련한 금액은 필수입니다.")
    @Min(value = 0, message = "현재 마련한 금액은 0원 이상이어야 합니다.")
    @Max(value = 1_000_000_000_000L, message = "현재 마련한 금액은 1조 원 이하여야 합니다.")
    private Long currentAmount;

    @NotNull(message = "목표 기간은 필수입니다.")
    @Min(value = 1, message = "목표 기간은 1개월 이상이어야 합니다.")
    @Max(value = 600, message = "목표 기간은 600개월 이하여야 합니다.")
    private Integer targetMonths;

}
