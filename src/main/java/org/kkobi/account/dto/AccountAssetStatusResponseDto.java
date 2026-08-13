package org.kkobi.account.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountAssetStatusResponseDto {

    private Long accountId;
    private BigDecimal seedMoney;
    private BigDecimal cashBalance;
    private BigDecimal stockAsset;
    private BigDecimal savingsAsset;
    private BigDecimal totalAsset;
    private BigDecimal totalProfit;
    private BigDecimal totalReturnRate;
    private BigDecimal cashRatio;
    private BigDecimal stockRatio;
    private BigDecimal savingsRatio;
}
