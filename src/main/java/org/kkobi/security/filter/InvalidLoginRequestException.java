package org.kkobi.security.filter;

import org.springframework.security.authentication.AuthenticationServiceException;

/** 로그인 요청 본문을 읽을 수 없거나 입력값 검증에 실패했을 때 발생하는 예외 */
public class InvalidLoginRequestException extends AuthenticationServiceException {
    public InvalidLoginRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
