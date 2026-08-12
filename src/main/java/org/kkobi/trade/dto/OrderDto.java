package org.kkobi.trade.dto;

import lombok.Data;
import org.kkobi.trade.enums.OrderMethod;
import org.kkobi.trade.enums.OrderStatus;
import org.kkobi.trade.enums.OrderType;

import java.time.LocalDateTime;

@Data
public class OrderDto {
    private Long securityOrderId;
    private Long accountId;
    private Long securityId;
    private String ticker;
    private String name;
    private OrderType orderType;
    private OrderMethod orderMethod;
    private Long orderPrice;
    private Long executedPrice;
    private Integer quantity;
    private Long executedAmount;
    private OrderStatus status;
    private LocalDateTime orderedAt;
    private LocalDateTime executedAt;
}
