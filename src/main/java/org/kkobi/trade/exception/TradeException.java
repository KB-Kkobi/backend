package org.kkobi.trade.exception;

public class TradeException extends RuntimeException {

    private final TradeErrorCode errorCode;

    public TradeException(TradeErrorCode errorCode) {
        super(errorCode.name());
        this.errorCode = errorCode;
    }

    public TradeErrorCode getErrorCode() {
        return errorCode;
    }
}
