package com.elevenst.realtimechat.domain.category.exception;

import com.elevenst.realtimechat.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CategoryErrorCode implements ErrorCode {
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "Category not found."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "Category information is invalid.");

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
