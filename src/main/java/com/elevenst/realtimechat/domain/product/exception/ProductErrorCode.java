package com.elevenst.realtimechat.domain.product.exception;

import com.elevenst.realtimechat.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ProductErrorCode implements ErrorCode {
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "상품을 찾을 수 없습니다."),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다."),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "상품은 중분류 카테고리에만 등록할 수 있습니다."),
    INVALID_SELLER(HttpStatus.BAD_REQUEST, "판매자 식별 정보가 올바르지 않습니다."),
    INVALID_PRODUCT_NAME(HttpStatus.BAD_REQUEST, "상품명이 올바르지 않습니다."),
    INVALID_PRICE(HttpStatus.BAD_REQUEST, "상품 가격은 0보다 커야 합니다."),
    INVALID_STOCK_QUANTITY(HttpStatus.BAD_REQUEST, "재고 수량은 0 이상이어야 합니다."),
    INVALID_SALE_STATUS(HttpStatus.BAD_REQUEST, "판매 상태와 재고 수량이 올바르지 않습니다."),
    PRODUCT_OWNER_MISMATCH(HttpStatus.FORBIDDEN, "해당 판매자의 상품이 아닙니다.");

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
