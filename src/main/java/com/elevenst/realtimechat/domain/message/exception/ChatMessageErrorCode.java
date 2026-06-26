package com.elevenst.realtimechat.domain.message.exception;

import com.elevenst.realtimechat.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ChatMessageErrorCode implements ErrorCode {
    INVALID_MESSAGE_TYPE(HttpStatus.BAD_REQUEST, "Chat message type is invalid."),
    PRODUCT_REFERENCE_REQUIRED(HttpStatus.BAD_REQUEST, "Product reference message requires productId."),
    PRODUCT_REFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Product reference not found."),
    PUBLISH_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "Chat message delivery is temporarily unavailable.");

    private final HttpStatus httpStatus;
    private final String message;

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String message() {
        return message;
    }
}
