package org.kkobi.trade.dto;

import lombok.Data;

@Data
public class TradeStockDto {
    private Long securityId;
    private String ticker;
    private String name;
    private String market;
    private String kisCode;
}
