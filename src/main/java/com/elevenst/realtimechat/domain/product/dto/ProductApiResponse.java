package com.elevenst.realtimechat.domain.product.dto;

public record ProductApiResponse<T>(
        String message,
        T data
) {

    public static <T> ProductApiResponse<T> success(String message, T data) {
        return new ProductApiResponse<>(message, data);
    }
}
