package com.elevenst.realtimechat.domain.promotion.exception;

import com.elevenst.realtimechat.global.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum CouponErrorCode implements ErrorCode {
    COUPON_001(HttpStatus.CONFLICT, "쿠폰을 발급할 수 있는 상태가 아닙니다."),
    COUPON_002(HttpStatus.CONFLICT, "선착순 수량이 모두 소진되었습니다."),
    COUPON_003(HttpStatus.CONFLICT, "이미 발급받았거나 중복 발급 요청입니다."),
    INVALID_COUPON_NAME(HttpStatus.BAD_REQUEST, "쿠폰명이 올바르지 않습니다."),
    INVALID_DISCOUNT_TYPE(HttpStatus.BAD_REQUEST, "할인 방식이 올바르지 않습니다."),
    INVALID_DISCOUNT_VALUE(HttpStatus.BAD_REQUEST, "할인 값이 올바르지 않습니다."),
    INVALID_MAX_DISCOUNT_AMOUNT(HttpStatus.BAD_REQUEST, "최대 할인 금액이 올바르지 않습니다."),
    INVALID_ISSUE_PERIOD(HttpStatus.BAD_REQUEST, "발급 종료 시각은 시작 시각보다 이후여야 합니다."),
    INVALID_TOTAL_QUANTITY(HttpStatus.BAD_REQUEST, "선착순 총 수량은 1개 이상이어야 합니다."),
    COUPON_POLICY_NOT_FOUND(HttpStatus.NOT_FOUND, "쿠폰 정책을 찾을 수 없습니다."),
    UNAUTHORIZED_ADMIN(HttpStatus.FORBIDDEN, "쿠폰 정책은 SUPER_ADMIN 만 등록할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    CouponErrorCode(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }

    @Override
    public String message() {
        return message;
    }
}
