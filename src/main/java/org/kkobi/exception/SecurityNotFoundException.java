package org.kkobi.exception;

// 요청한 ticker에 해당하는 종목이 없을 때 발생
public class SecurityNotFoundException extends RuntimeException {

    public SecurityNotFoundException(String message) {
        super(message);
    }
}
