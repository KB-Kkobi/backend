package org.kkobi.trade.exception;

public enum TradeErrorCode {
    INVALID_QUANTITY(400),
    INVALID_PRICE(400),
    PRICE_REQUIRED_FOR_LIMIT(400),
    INSUFFICIENT_CASH(400),
    INSUFFICIENT_QUANTITY(400),
    SECURITY_NOT_FOUND(404),
    MARKET_CLOSED(409),
    QUOTE_UNAVAILABLE(503),
    ORDER_NOT_FOUND(404),
    ORDER_NOT_CANCELABLE(409),
    FORBIDDEN_ORDER(403);

    public final int httpStatus;

    TradeErrorCode(int httpStatus) {
        this.httpStatus = httpStatus;
    }
}
