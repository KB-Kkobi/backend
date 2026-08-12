package org.kkobi.trade.dto;

import lombok.Data;

@Data
public class HoldingDto {
    private Long holdingSecurityId;
    private Long accountId;
    private Long securityId;
    private Integer quantity;
    private Integer lockedQuantity;
    private Long averagePrice;
}
