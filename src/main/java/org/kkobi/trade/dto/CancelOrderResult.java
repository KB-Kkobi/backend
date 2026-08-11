package org.kkobi.trade.dto;

import lombok.Data;
import org.kkobi.trade.enums.OrderStatus;

import java.time.LocalDateTime;

@Data
public class CancelOrderResult {
    private Long securityOrderId;
    private OrderStatus status;
    private LocalDateTime updatedAt;
}
