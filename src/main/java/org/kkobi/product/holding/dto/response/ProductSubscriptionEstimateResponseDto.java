package org.kkobi.product.holding.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductSubscriptionEstimateResponseDto {

    private Long productOptionId;
    private String productType;
    private String financialCompanyName;
    private String productName;
    private BigDecimal joinAmount;
    private BigDecimal appliedRate;
    private Integer savingTerm;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate maturityDate;

    private BigDecimal expectedPrincipal;
    private BigDecimal expectedInterest;
    private BigDecimal expectedInterestTax;
    private BigDecimal expectedAfterTaxInterest;
    private BigDecimal expectedMaturityAmount;
}
