package org.kkobi.account.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountAssetInfoDto {

    private Long accountId;
    private BigDecimal seedMoney;
    private BigDecimal cashBalance;
    private BigDecimal stockAsset;
}
