package org.kkobi.trade.event;

import org.kkobi.trade.enums.OrderType;

public record TradeOrderFilledEvent(Long accountId,
                                    Long securityOrderId,
                                    Long securityId,
                                    OrderType orderType,
                                    Integer quantity,
                                    Long executedPrice) {
}
