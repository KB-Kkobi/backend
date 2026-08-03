package org.kkobi.exception;

// 이메일 또는 닉네임이 이미 사용 중일 때 발생하는 예외
public class DuplicateUserException extends RuntimeException {

    public DuplicateUserException(String message) {
        super(message);
    }
}
