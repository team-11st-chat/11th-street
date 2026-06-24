package com.elevenst.realtimechat.global.response;

public record ApiResponse<T>(String message, T data) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("성공", data);
    }

    public static ApiResponse<Void> fail(String message) {
        return new ApiResponse<>(message, null);
    }
}
