package com.elevenst.realtimechat.global.response;

public record ApiResponse<T>(
        String message,
        T data
) {

    private static final String DEFAULT_SUCCESS_MESSAGE = "요청이 성공했습니다.";

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(DEFAULT_SUCCESS_MESSAGE, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(DEFAULT_SUCCESS_MESSAGE, data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(message, data);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(message, null);
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(message, data);
    }
}
