package org.kkobi.trade.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SecurityOrderFilledEvent(
        Long userId,
        Long accountId,
        Long securityOrderId,
        String actionType,
        Long securityId,
        String stockCode,
        Integer quantity,
        Long actionAmount,
        LocalDateTime tradedAt,
        BigDecimal currentPriceChangeRate,
        BigDecimal dailyPriceRangeRate
) {}
