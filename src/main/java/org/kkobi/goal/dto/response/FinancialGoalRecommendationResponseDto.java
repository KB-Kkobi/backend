package org.kkobi.goal.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.kkobi.product.dto.response.ProductListResponseDto;

@Getter
@Builder
public class FinancialGoalRecommendationResponseDto {
    private final FinancialGoalResponseDto goal;
    private final ProductListResponseDto products;
}
