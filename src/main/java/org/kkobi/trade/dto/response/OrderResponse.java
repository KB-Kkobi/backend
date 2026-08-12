package org.kkobi.trade.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.kkobi.trade.enums.OrderMethod;
import org.kkobi.trade.enums.OrderStatus;
import org.kkobi.trade.enums.OrderType;

import java.time.OffsetDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.ALWAYS)
public class OrderResponse {
    private Long securityOrderId;
    private Long securityId;
    private String ticker;
    private String name;
    private OrderType orderType;
    private OrderMethod orderMethod;
    private Long orderPrice;
    private Long executedPrice;
    private int quantity;
    private Long executedAmount;
    private OrderStatus status;
    private OffsetDateTime orderedAt;
    private OffsetDateTime executedAt;
}
