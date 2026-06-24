package com.elevenst.realtimechat.global.exception;

import com.elevenst.realtimechat.global.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();

        return ResponseEntity
                .status(errorCode.httpStatus())
                .body(ApiResponse.error(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ValidationErrorResponse>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception
    ) {
        List<ValidationErrorResponse> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(ValidationErrorResponse::from)
                .toList();

        return ResponseEntity
                .status(CommonErrorCode.INVALID_REQUEST.httpStatus())
                .body(ApiResponse.error(CommonErrorCode.INVALID_REQUEST.message(), errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<ValidationErrorResponse>>> handleConstraintViolationException(
            ConstraintViolationException exception
    ) {
        List<ValidationErrorResponse> errors = exception.getConstraintViolations()
                .stream()
                .map(violation -> new ValidationErrorResponse(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        return ResponseEntity
                .status(CommonErrorCode.INVALID_REQUEST.httpStatus())
                .body(ApiResponse.error(CommonErrorCode.INVALID_REQUEST.message(), errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException exception
    ) {
        String message = exception.getName() + " 값의 형식이 올바르지 않습니다.";

        return ResponseEntity
                .status(CommonErrorCode.INVALID_REQUEST.httpStatus())
                .body(ApiResponse.error(message));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException() {
        return ResponseEntity
                .status(CommonErrorCode.METHOD_NOT_ALLOWED.httpStatus())
                .body(ApiResponse.error(CommonErrorCode.METHOD_NOT_ALLOWED.message()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception exception) {
        log.error("Unhandled exception occurred", exception);

        return ResponseEntity
                .status(CommonErrorCode.INTERNAL_SERVER_ERROR.httpStatus())
                .body(ApiResponse.error(CommonErrorCode.INTERNAL_SERVER_ERROR.message()));
    }
}
