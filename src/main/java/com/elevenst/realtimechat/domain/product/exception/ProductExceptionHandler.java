package com.elevenst.realtimechat.domain.product.exception;

import com.elevenst.realtimechat.domain.product.dto.ProductApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.elevenst.realtimechat.domain.product")
public class ProductExceptionHandler {

    @ExceptionHandler(ProductException.class)
    public ResponseEntity<ProductApiResponse<Void>> handleProductException(ProductException exception) {
        ProductErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ProductApiResponse<>(errorCode.getMessage(), null));
    }
}
