package org.kkobi.assessment.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AccountDailySnapshotDto {

    private Long accountDailySnapshotId;
    private Long accountId;
    private Long userId;
    private LocalDate virtualInvestmentStartedDate;
    private LocalDate securityHoldingStartedDate;
    private LocalDate snapshotDate;
    private Long currentCash;
    private Long currentStockPrincipal;
    private Long currentDeposit;
    private Long totalInvestedPrincipal;
    private BigDecimal cashRatio;
    private BigDecimal averageSecurityHoldingDays;
}
