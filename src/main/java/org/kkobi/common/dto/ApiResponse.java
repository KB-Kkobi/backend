package org.kkobi.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 매매 API 공통 응답 래퍼.
 * 성공: {"success": true, "data": {...}}
 * 실패: {"success": false, "error": {"code": "...", "message": "..."}}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorBody error;

    private ApiResponse(boolean success, T data, ErrorBody error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message));
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ErrorBody getError() {
        return error;
    }

    public record ErrorBody(String code, String message) {}
}
