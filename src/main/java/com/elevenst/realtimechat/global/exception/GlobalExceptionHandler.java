package com.elevenst.realtimechat.global.exception;

import com.elevenst.realtimechat.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * STUB: 전역 예외 처리(공통 예외 모델·응답 변환)는 플랫폼 공통(global) 작업 단위 소유로, 이슈 #21 범위 밖이다.
 * 회원/인증 API 가 문서화된 상태코드(400/401/409)를 반환하도록 하는 최소 임시 구현이다.
 *
 * TODO(실제 구현 교체 지점): 플랫폼 공통의 정식 전역 예외 처리/에러 응답 규격으로 교체.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleServiceException(ServiceException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("잘못된 요청입니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(message));
    }
}
