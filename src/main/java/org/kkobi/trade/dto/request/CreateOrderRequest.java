package org.kkobi.trade.dto.request;

import lombok.Data;
import org.kkobi.trade.enums.OrderMethod;
import org.kkobi.trade.enums.OrderType;

@Data
public class CreateOrderRequest {
    private Long securityId;
    private OrderType orderType;
    private OrderMethod orderMethod;
    private Long price;
    private Integer quantity;
}
