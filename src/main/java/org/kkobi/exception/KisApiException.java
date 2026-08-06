package org.kkobi.exception;

// 한국투자증권(KIS) Open API 호출 실패 시 발생하는 예외
public class KisApiException extends RuntimeException {

    public KisApiException(String message) {
        super(message);
    }

    public KisApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
