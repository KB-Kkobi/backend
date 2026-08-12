package org.kkobi.trade.dto;

import lombok.Data;

@Data
public class TradeAccountDto {
    private Long accountId;
    private Long userId;
    private Long cashBalance;
    private Long lockedCash;
    private Long seedMoney;
}
