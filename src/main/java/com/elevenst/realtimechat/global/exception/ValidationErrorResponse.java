package com.elevenst.realtimechat.global.exception;

import org.springframework.validation.FieldError;

public record ValidationErrorResponse(
        String field,
        String message
) {

    public static ValidationErrorResponse from(FieldError fieldError) {
        return new ValidationErrorResponse(fieldError.getField(), fieldError.getDefaultMessage());
    }
}
