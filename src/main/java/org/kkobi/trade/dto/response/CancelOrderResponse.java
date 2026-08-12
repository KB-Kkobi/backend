package org.kkobi.trade.dto.response;

import lombok.Builder;
import lombok.Getter;
import org.kkobi.trade.enums.OrderStatus;

import java.time.OffsetDateTime;

@Getter
@Builder
public class CancelOrderResponse {
    private Long securityOrderId;
    private OrderStatus status;
    private OffsetDateTime updatedAt;
}
