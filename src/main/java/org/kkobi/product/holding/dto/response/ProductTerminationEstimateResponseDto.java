package org.kkobi.product.holding.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class ProductTerminationEstimateResponseDto {

    private Long holdingProductId;
    private String productType;
    private String financialCompanyName;
    private String productName;
    private BigDecimal joinAmount;
    private BigDecimal appliedRate;
    private Integer savingTerm;
    private Integer totalInstallments;
    private Integer paidInstallments;
    private Integer remainingInstallments;
    private BigDecimal remainingContributionAmount;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate maturityDate;

    private Long remainingDays;
    private BigDecimal maturityProgressRate;
    private BigDecimal currentPrincipal;
    private BigDecimal terminationInterest;
    private BigDecimal terminationInterestTax;
    private BigDecimal terminationAfterTaxInterest;
    private BigDecimal terminationRefundAmount;
    private BigDecimal expectedMaturityPrincipal;
    private BigDecimal expectedMaturityInterest;
    private BigDecimal expectedMaturityInterestTax;
    private BigDecimal expectedMaturityAfterTaxInterest;
    private BigDecimal expectedMaturityAmount;
    private BigDecimal foregoneInterest;
    private BigDecimal currentSavingsRatio;
    private BigDecimal afterTerminationSavingsRatio;
}
